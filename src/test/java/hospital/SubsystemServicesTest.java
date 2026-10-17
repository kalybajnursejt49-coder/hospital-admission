package hospital;

import exception.InsuranceRejectedException;
import exception.InvalidPatientDataException;
import exception.PharmacyOutOfStockException;
import orders.HospitalOrder;
import orders.LabOrderSystem;
import orders.OrderRepository;
import orders.PharmacyDispatcher;
import orders.StockChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import registration.Admission;
import registration.AdmissionRepository;
import registration.AdmissionValidator;
import registration.InsuranceVerifier;
import registration.RegistrationDesk;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SubsystemServicesTest {
    @TempDir Path folder;

    @Test
    void registrationDeskValidatesAndRegistersWithoutFacade() {
        AdmissionRepository repository = new AdmissionRepository(folder.resolve("hospital.db"));
        RegistrationDesk desk = new RegistrationDesk(new AdmissionValidator(), new InsuranceVerifier(), repository);
        assertThrows(InvalidPatientDataException.class, () -> desk.register("  ", "INS-100"));
        assertThrows(InsuranceRejectedException.class, () -> desk.register("Aibek", "BAD"));
        assertTrue(repository.findAll().isEmpty());
        Admission admission = desk.register(" Aibek ", "INS-100");
        assertEquals("Aibek", admission.getPatientName());
        assertEquals(Admission.Status.REGISTERED, repository.findById(admission.getId()).getStatus());
    }

    @Test
    void labOrderSystemSendsOrderWithoutFacade() {
        OrderRepository repository = new OrderRepository(folder.resolve("hospital.db"));
        HospitalOrder order = new LabOrderSystem(repository).createAndSend(42);
        assertEquals("LAB", order.getType());
        assertEquals(HospitalOrder.Status.SENT, order.getStatus());
        assertEquals(HospitalOrder.Status.SENT, repository.findByAdmissionId(42).get(0).getStatus());
    }

    @Test
    void pharmacyDispatcherChecksStockWithoutFacade() {
        OrderRepository repository = new OrderRepository(folder.resolve("hospital.db"));
        PharmacyDispatcher dispatcher = new PharmacyDispatcher(repository, new StockChecker());
        repository.setMedicineStock("aspirin", 1);
        HospitalOrder order = dispatcher.dispatch(42, "aspirin");
        assertEquals(HospitalOrder.Status.SENT, order.getStatus());
        assertEquals(0, repository.getMedicineStock("aspirin"));
        assertThrows(PharmacyOutOfStockException.class, () -> dispatcher.dispatch(43, "aspirin"));
        assertEquals(1, repository.findAll().size());
    }
}
