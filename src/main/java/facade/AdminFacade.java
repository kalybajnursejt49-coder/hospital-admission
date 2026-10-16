package facade;

import bed.BedAssignment;
import orders.HospitalOrder;
import registration.Admission;

public class AdminFacade {
    private final AdmissionFacade admissionFacade;

    public AdminFacade(AdmissionFacade admissionFacade) {
        this.admissionFacade = admissionFacade;
    }

    public void setFreeBeds(String department, int count) {
        admissionFacade.getBedRepository().setFreeBeds(department, count);
    }

    public void setMedicineStock(String medicine, int count) {
        admissionFacade.getOrderRepository().setMedicineStock(medicine, count);
    }

    public void compensate(int admissionId) {
        admissionFacade.compensate(admissionId);
    }

    public void reset() {
        admissionFacade.getOrderRepository().reset();
        admissionFacade.getBedRepository().reset();
        admissionFacade.getAdmissionRepository().reset();
        admissionFacade.getChainRepository().reset();
    }

    public void printTables() {
        System.out.println("admissions:");
        for (Admission admission : admissionFacade.getAdmissionRepository().findAll()) {
            System.out.println(admission.getId() + " | " + admission.getPatientName() + " | " + admission.getInsuranceNumber() + " | " + admission.getStatus());
        }
        System.out.println("bed_assignments:");
        for (BedAssignment assignment : admissionFacade.getBedRepository().findAll()) {
            System.out.println(assignment.getAdmissionId() + " | " + assignment.getDepartment() + " | " + assignment.getEquipment() + " | " + assignment.getStatus());
        }
        System.out.println("hospital_orders:");
        for (HospitalOrder order : admissionFacade.getOrderRepository().findAll()) {
            System.out.println(order.getId() + " | " + order.getAdmissionId() + " | " + order.getType() + " | " + order.getMedicine() + " | " + order.getStatus());
        }
        System.out.println("admission_chain:");
        for (ChainRecord chain : admissionFacade.getChainRepository().findAll()) {
            System.out.println(chain.getAdmissionId() + " | " + chain.getStatus());
        }
        System.out.println("Свободные койки GENERAL: " + admissionFacade.getBedRepository().getFreeBeds("GENERAL"));
        System.out.println("Свободные койки ICU: " + admissionFacade.getBedRepository().getFreeBeds("ICU"));
        System.out.println("Остаток paracetamol: " + admissionFacade.getOrderRepository().getMedicineStock("paracetamol"));
    }
}
