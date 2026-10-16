package hospital;

import bed.BedAssignment;
import exception.BedUnavailableException;
import exception.InsuranceRejectedException;
import exception.InvalidPatientDataException;
import exception.PharmacyOutOfStockException;
import orders.HospitalOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import registration.Admission;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdmissionFacadeTest {
    @TempDir Path folder;

    @Test
    void successfulAdmissionCompletesAllStages() {
        HospitalTestKit kit = new HospitalTestKit(folder);
        int id = kit.admissions().admit("Aibek", "INS-100", "GENERAL", "paracetamol");
        kit.assertChainStatus(id, "COMPLETED");
        assertEquals(Admission.Status.REGISTERED, kit.admissions().getAdmissionRepository().findById(id).getStatus());
        assertEquals(BedAssignment.Status.ASSIGNED, kit.admissions().getBedRepository().findByAdmissionId(id).getStatus());
        assertEquals(2, kit.admissions().getOrderRepository().findByAdmissionId(id).size());
        assertEquals(HospitalOrder.Status.SENT, kit.admissions().getOrderRepository().findByAdmissionId(id).get(0).getStatus());
        assertEquals(2, kit.admissions().getBedRepository().getFreeBeds("GENERAL"));
        assertEquals(4, kit.admissions().getOrderRepository().getMedicineStock("paracetamol"));
    }

    @Test
    void noBedsCompensatesOnlyRegistration() {
        HospitalTestKit kit = new HospitalTestKit(folder).withNoFreeBeds();
        assertThrows(BedUnavailableException.class, () -> kit.admissions().admit("Aibek", "INS-100", "GENERAL", "paracetamol"));
        kit.assertChainStatus(1, "COMPENSATED");
        assertEquals(Admission.Status.CANCELLED, kit.admissions().getAdmissionRepository().findById(1).getStatus());
        assertEquals(null, kit.admissions().getBedRepository().findByAdmissionId(1));
        assertTrue(kit.admissions().getOrderRepository().findByAdmissionId(1).isEmpty());
    }

    @Test
    void noMedicineCancelsLabThenBedAndRegistration() {
        HospitalTestKit kit = new HospitalTestKit(folder).withNoMedicine();
        assertThrows(PharmacyOutOfStockException.class, () -> kit.admissions().admit("Aibek", "INS-100", "GENERAL", "paracetamol"));
        kit.assertChainStatus(1, "COMPENSATED");
        assertEquals(Admission.Status.CANCELLED, kit.admissions().getAdmissionRepository().findById(1).getStatus());
        assertEquals(BedAssignment.Status.RELEASED, kit.admissions().getBedRepository().findByAdmissionId(1).getStatus());
        assertEquals(3, kit.admissions().getBedRepository().getFreeBeds("GENERAL"));
        assertEquals(HospitalOrder.Status.CANCELLED, kit.admissions().getOrderRepository().findByAdmissionId(1).get(0).getStatus());
    }

    @Test
    void invalidDataAndInsuranceNeverWriteAnything() {
        HospitalTestKit kit = new HospitalTestKit(folder);
        assertThrows(InvalidPatientDataException.class, () -> kit.admissions().admit("", "INS-100", "GENERAL", "paracetamol"));
        assertThrows(InvalidPatientDataException.class, () -> kit.admissions().admit("Aibek", "", "GENERAL", "paracetamol"));
        assertThrows(InsuranceRejectedException.class, () -> kit.admissions().admit("Aibek", "BAD", "GENERAL", "paracetamol"));
        assertThrows(InvalidPatientDataException.class, () -> kit.admissions().admit("Aibek", "INS-100", "", "paracetamol"));
        assertThrows(InvalidPatientDataException.class, () -> kit.admissions().admit("Aibek", "INS-100", "GENERAL", ""));
        assertTrue(kit.admissions().getAdmissionRepository().findAll().isEmpty());
        assertTrue(kit.admissions().getChainRepository().findAll().isEmpty());
    }

    @Test
    void manualCompensationRestoresResourcesOnlyOnce() {
        HospitalTestKit kit = new HospitalTestKit(folder);
        int id = kit.admissions().admit("Aibek", "INS-100", "ICU", "paracetamol");
        kit.admin().compensate(id);
        kit.admin().compensate(id);
        kit.assertChainStatus(id, "COMPENSATED");
        assertEquals(1, kit.admissions().getBedRepository().getFreeBeds("ICU"));
        assertEquals(5, kit.admissions().getOrderRepository().getMedicineStock("paracetamol"));
        assertEquals(2, kit.admissions().getOrderRepository().findByAdmissionId(id).size());
        assertEquals(HospitalOrder.Status.CANCELLED, kit.admissions().getOrderRepository().findByAdmissionId(id).get(1).getStatus());
    }
}
