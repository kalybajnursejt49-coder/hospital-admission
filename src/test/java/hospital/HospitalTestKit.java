package hospital;

import facade.AdminFacade;
import facade.AdmissionFacade;
import facade.ChainStatus;

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
        assertEquals(ChainStatus.valueOf(status), admissions.getChainRepository().getStatus(id));
    }

    public void restart() {
        admissions = new AdmissionFacade(file);
        admin = new AdminFacade(admissions);
    }

    public AdmissionFacade admissions() { return admissions; }
    public AdminFacade admin() { return admin; }
    public Path file() { return file; }
}
