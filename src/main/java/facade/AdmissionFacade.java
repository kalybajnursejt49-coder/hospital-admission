package facade;

import bed.BedAllocator;
import bed.BedRepository;
import bed.EquipmentPolicy;
import bed.EquipmentReserver;
import database.Sqlite;
import exception.BedUnavailableException;
import exception.InvalidPatientDataException;
import exception.PharmacyOutOfStockException;
import orders.LabOrderSystem;
import orders.OrderRepository;
import orders.PharmacyDispatcher;
import orders.StockChecker;
import registration.Admission;
import registration.AdmissionRepository;
import registration.AdmissionValidator;
import registration.InsuranceVerifier;
import registration.RegistrationDesk;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public class AdmissionFacade {
    private final AdmissionRepository admissionRepository;
    private final BedRepository bedRepository;
    private final OrderRepository orderRepository;
    private final ChainRepository chainRepository;
    private final RegistrationDesk registrationDesk;
    private final BedAllocator bedAllocator;
    private final EquipmentReserver equipmentReserver;
    private final LabOrderSystem labOrderSystem;
    private final PharmacyDispatcher pharmacyDispatcher;
    private final Path databaseFile;

    public AdmissionFacade(Path databaseFile) {
        this.databaseFile = databaseFile;
        admissionRepository = new AdmissionRepository(databaseFile);
        bedRepository = new BedRepository(databaseFile);
        orderRepository = new OrderRepository(databaseFile);
        chainRepository = new ChainRepository(databaseFile);
        registrationDesk = new RegistrationDesk(new AdmissionValidator(), new InsuranceVerifier(), admissionRepository);
        bedAllocator = new BedAllocator(bedRepository, new EquipmentPolicy());
        equipmentReserver = new EquipmentReserver(bedRepository);
        labOrderSystem = new LabOrderSystem(orderRepository);
        pharmacyDispatcher = new PharmacyDispatcher(orderRepository, new StockChecker());
    }

    public int admit(String name, String insuranceNumber, String department, String medicine) {
        if (department == null || department.trim().isEmpty()) {
            throw new InvalidPatientDataException("Отделение не указано");
        }
        if (medicine == null || medicine.trim().isEmpty()) {
            throw new InvalidPatientDataException("Препарат не указан");
        }
        int id;
        try (Connection connection = Sqlite.open(databaseFile)) {
            connection.setAutoCommit(false);
            try {
                Admission admission = registrationDesk.register(connection, name, insuranceNumber);
                id = admission.getId();
                chainRepository.savePendingB(connection, id);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Sqlite.failure(e);
        }
        try {
            bedAllocator.reserve(id, department);
            equipmentReserver.reserve(id);
            chainRepository.setStatus(id, ChainStatus.PENDING_C);
        } catch (BedUnavailableException e) {
            bedRepository.release(id);
            admissionRepository.cancelAdmission(id);
            chainRepository.setStatus(id, ChainStatus.COMPENSATED);
            throw e;
        }
        try {
            labOrderSystem.createAndSend(id);
            pharmacyDispatcher.dispatch(id, medicine);
            chainRepository.setStatus(id, ChainStatus.COMPLETED);
        } catch (PharmacyOutOfStockException e) {
            orderRepository.cancelByAdmissionId(id);
            bedRepository.release(id);
            admissionRepository.cancelAdmission(id);
            chainRepository.setStatus(id, ChainStatus.COMPENSATED);
            throw e;
        }
        return id;
    }

    public int recoverUnfinished() {
        int recovered = 0;
        for (ChainRecord chain : chainRepository.findUnfinished()) {
            compensate(chain.getAdmissionId());
            recovered++;
        }
        return recovered;
    }

    public void compensate(int admissionId) {
        if (chainRepository.getStatus(admissionId) == null) {
            throw new IllegalArgumentException("Цепочка не найдена: " + admissionId);
        }
        orderRepository.cancelByAdmissionId(admissionId);
        bedRepository.release(admissionId);
        admissionRepository.cancelAdmission(admissionId);
        chainRepository.setStatus(admissionId, ChainStatus.COMPENSATED);
    }
}
