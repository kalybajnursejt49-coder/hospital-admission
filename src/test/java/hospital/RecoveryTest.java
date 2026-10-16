package hospital;

import bed.BedAllocator;
import bed.BedAssignment;
import bed.EquipmentPolicy;
import bed.EquipmentReserver;
import facade.ChainStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import registration.Admission;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecoveryTest {
    @TempDir Path folder;

    @Test
    void pendingBAfterRestartCancelsRegistration() {
        HospitalTestKit kit = new HospitalTestKit(folder);
        Admission admission = kit.admissions().getAdmissionRepository().saveAdmission("Aibek", "INS-100");
        kit.admissions().getChainRepository().savePendingB(admission.getId());
        kit.restart();
        assertEquals(1, kit.admissions().recoverUnfinished());
        kit.assertChainStatus(admission.getId(), "COMPENSATED");
        assertEquals(Admission.Status.CANCELLED, kit.admissions().getAdmissionRepository().findById(admission.getId()).getStatus());
        assertEquals(0, kit.admissions().recoverUnfinished());
    }

    @Test
    void pendingCAfterRestartReleasesBedAndCancelsRegistration() {
        HospitalTestKit kit = new HospitalTestKit(folder);
        Admission admission = kit.admissions().getAdmissionRepository().saveAdmission("Aibek", "INS-100");
        int id = admission.getId();
        kit.admissions().getChainRepository().savePendingB(id);
        new BedAllocator(kit.admissions().getBedRepository(), new EquipmentPolicy()).reserve(id, "ICU");
        new EquipmentReserver(kit.admissions().getBedRepository()).reserve(id);
        kit.admissions().getChainRepository().setStatus(id, ChainStatus.PENDING_C);
        assertEquals(0, kit.admissions().getBedRepository().getFreeBeds("ICU"));
        kit.restart();
        assertEquals(1, kit.admissions().recoverUnfinished());
        kit.assertChainStatus(id, "COMPENSATED");
        assertEquals(Admission.Status.CANCELLED, kit.admissions().getAdmissionRepository().findById(id).getStatus());
        assertEquals(BedAssignment.Status.RELEASED, kit.admissions().getBedRepository().findByAdmissionId(id).getStatus());
        assertEquals(1, kit.admissions().getBedRepository().getFreeBeds("ICU"));
    }
}
