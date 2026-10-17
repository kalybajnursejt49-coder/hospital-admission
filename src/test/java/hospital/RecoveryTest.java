package hospital;

import bed.BedAllocator;
import bed.BedAssignment;
import bed.EquipmentPolicy;
import bed.EquipmentReserver;
import database.Sqlite;
import facade.ChainStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import registration.Admission;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecoveryTest {
    @TempDir Path folder;

    @Test
    void pendingBAfterRestartCancelsRegistration() {
        HospitalTestKit kit = new HospitalTestKit(folder);
        Admission admission = kit.registrations().saveAdmission("Aibek", "INS-100");
        kit.chains().savePendingB(admission.getId());
        kit.restart();
        assertEquals(1, kit.admissions().recoverUnfinished());
        kit.assertChainStatus(admission.getId(), "COMPENSATED");
        assertEquals(Admission.Status.CANCELLED, kit.registrations().findById(admission.getId()).getStatus());
        assertEquals(0, kit.admissions().recoverUnfinished());
    }

    @Test
    void pendingCAfterRestartReleasesBedAndCancelsRegistration() {
        HospitalTestKit kit = new HospitalTestKit(folder);
        Admission admission = kit.registrations().saveAdmission("Aibek", "INS-100");
        int id = admission.getId();
        kit.chains().savePendingB(id);
        new BedAllocator(kit.beds(), new EquipmentPolicy()).reserve(id, "ICU");
        new EquipmentReserver(kit.beds()).reserve(id);
        kit.chains().setStatus(id, ChainStatus.PENDING_C);
        assertEquals(0, kit.beds().getFreeBeds("ICU"));
        kit.restart();
        assertEquals(1, kit.admissions().recoverUnfinished());
        kit.assertChainStatus(id, "COMPENSATED");
        assertEquals(Admission.Status.CANCELLED, kit.registrations().findById(id).getStatus());
        assertEquals(BedAssignment.Status.RELEASED, kit.beds().findByAdmissionId(id).getStatus());
        assertEquals(1, kit.beds().getFreeBeds("ICU"));
    }

    @Test
    void missingChainTableRollsBackRegistration() throws Exception {
        HospitalTestKit kit = new HospitalTestKit(folder);
        try (Connection connection = Sqlite.open(kit.file()); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE admission_chain");
        }
        assertThrows(IllegalStateException.class, () -> kit.admissions().admit("Aibek", "INS-100", "GENERAL", "paracetamol"));
        assertTrue(kit.registrations().findAll().isEmpty());
        assertTrue(kit.chains().findAll().isEmpty());
    }
}
