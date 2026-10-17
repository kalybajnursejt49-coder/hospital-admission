package hospital;

import facade.AdminFacade;
import facade.AdmissionFacade;
import facade.ChainStatus;
import facade.ChainRepository;
import bed.BedRepository;
import orders.OrderRepository;
import registration.AdmissionRepository;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HospitalTestKit {
    private final Path file;
    private AdmissionFacade admissions;
    private AdminFacade admin;

    public HospitalTestKit(Path folder) {
        file = folder.resolve("hospital.db");
        restart();
    }

    public HospitalTestKit givenPatient(String name) {
        admissions.admit(name, "INS-100", "GENERAL", "paracetamol");
        return this;
    }

    public HospitalTestKit withNoFreeBeds() {
        admin.setFreeBeds("GENERAL", 0);
        return this;
    }

    public HospitalTestKit withNoMedicine() {
        admin.setMedicineStock("paracetamol", 0);
        return this;
    }

    public void assertChainStatus(int id, String status) {
        assertEquals(ChainStatus.valueOf(status), chains().getStatus(id));
    }

    public void restart() {
        admissions = new AdmissionFacade(file);
        admin = new AdminFacade(file, admissions);
    }

    public AdmissionFacade admissions() { return admissions; }
    public AdminFacade admin() { return admin; }
    public Path file() { return file; }
    public AdmissionRepository registrations() { return new AdmissionRepository(file); }
    public BedRepository beds() { return new BedRepository(file); }
    public OrderRepository orders() { return new OrderRepository(file); }
    public ChainRepository chains() { return new ChainRepository(file); }
}
