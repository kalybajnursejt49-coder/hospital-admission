package facade;

import bed.BedAssignment;
import bed.BedRepository;
import orders.HospitalOrder;
import orders.OrderRepository;
import registration.Admission;
import registration.AdmissionRepository;

import java.nio.file.Path;

public class AdminFacade {
    private final AdmissionFacade admissionFacade;
    private final AdmissionRepository admissionRepository;
    private final BedRepository bedRepository;
    private final OrderRepository orderRepository;
    private final ChainRepository chainRepository;

    public AdminFacade(Path databaseFile, AdmissionFacade admissionFacade) {
        this.admissionFacade = admissionFacade;
        admissionRepository = new AdmissionRepository(databaseFile);
        bedRepository = new BedRepository(databaseFile);
        orderRepository = new OrderRepository(databaseFile);
        chainRepository = new ChainRepository(databaseFile);
    }

    public void setFreeBeds(String department, int count) {
        bedRepository.setFreeBeds(department, count);
    }

    public void setMedicineStock(String medicine, int count) {
        orderRepository.setMedicineStock(medicine, count);
    }

    public void compensate(int admissionId) {
        admissionFacade.compensate(admissionId);
    }

    public void reset() {
        orderRepository.reset();
        bedRepository.reset();
        admissionRepository.reset();
        chainRepository.reset();
    }

    public void printTables() {
        System.out.println("admissions:");
        for (Admission admission : admissionRepository.findAll()) {
            System.out.println(admission.getId() + " | " + admission.getPatientName() + " | " + admission.getInsuranceNumber() + " | " + admission.getStatus());
        }
        System.out.println("bed_assignments:");
        for (BedAssignment assignment : bedRepository.findAll()) {
            System.out.println(assignment.getAdmissionId() + " | " + assignment.getDepartment() + " | " + assignment.getEquipment() + " | " + assignment.getStatus());
        }
        System.out.println("hospital_orders:");
        for (HospitalOrder order : orderRepository.findAll()) {
            System.out.println(order.getId() + " | " + order.getAdmissionId() + " | " + order.getType() + " | " + order.getMedicine() + " | " + order.getStatus());
        }
        System.out.println("admission_chain:");
        for (ChainRecord chain : chainRepository.findAll()) {
            System.out.println(chain.getAdmissionId() + " | " + chain.getStatus());
        }
        System.out.println("Свободные койки GENERAL: " + bedRepository.getFreeBeds("GENERAL"));
        System.out.println("Свободные койки ICU: " + bedRepository.getFreeBeds("ICU"));
        System.out.println("Остаток paracetamol: " + orderRepository.getMedicineStock("paracetamol"));
    }
}
