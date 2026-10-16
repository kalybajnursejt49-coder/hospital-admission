package hospital;

import bed.BedAssignment;
import bed.BedRepository;
import database.Sqlite;
import exception.BedUnavailableException;
import exception.PharmacyOutOfStockException;
import orders.HospitalOrder;
import orders.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import registration.Admission;
import registration.AdmissionRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SubsystemTest {
    @TempDir Path folder;

    @Test
    void reservedBedCanBeReleasedWithoutTakingEquipment() {
        BedRepository beds = new BedRepository(folder.resolve("hospital.db"));
        BedAssignment assignment = beds.reserve(1, "general", "BASIC");
        assertEquals(BedAssignment.Status.RESERVED, assignment.getStatus());
        beds.release(1);
        beds.release(1);
        assertEquals(3, beds.getFreeBeds("GENERAL"));
        assertEquals(BedAssignment.Status.RELEASED, beds.findByAdmissionId(1).getStatus());
        assertEquals(1, beds.findAll().size());
    }

    @Test
    void missingEquipmentDoesNotConsumeBedAfterCompensation() throws Exception {
        Path file = folder.resolve("hospital.db");
        BedRepository beds = new BedRepository(file);
        try (Connection connection = Sqlite.open(file); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE equipment_stock SET free_count = 0 WHERE equipment = 'MONITOR'");
        }
        beds.reserve(1, "ICU", "MONITOR");
        assertThrows(BedUnavailableException.class, () -> beds.assignEquipment(1));
        beds.release(1);
        assertEquals(1, beds.getFreeBeds("ICU"));
        assertThrows(BedUnavailableException.class, () -> beds.reserve(2, "UNKNOWN", "BASIC"));
        assertNull(beds.findByAdmissionId(2));
    }

    @Test
    void cancelledOrdersRestoreStockOnlyOnce() {
        OrderRepository orders = new OrderRepository(folder.resolve("hospital.db"));
        HospitalOrder lab = orders.saveOrder(1, "LAB", "");
        assertEquals(HospitalOrder.Status.CREATED, lab.getStatus());
        orders.markSent(lab.getId());
        orders.setMedicineStock("aspirin", 1);
        assertEquals(1, orders.getMedicineStock("aspirin"));
        HospitalOrder pharmacy = orders.sendPharmacyOrder(1, "aspirin");
        assertEquals(HospitalOrder.Status.SENT, pharmacy.getStatus());
        assertThrows(PharmacyOutOfStockException.class, () -> orders.sendPharmacyOrder(2, "aspirin"));
        orders.cancelByAdmissionId(1);
        orders.cancelByAdmissionId(1);
        assertEquals(1, orders.getMedicineStock("aspirin"));
        assertEquals(2, orders.findAll().size());
        assertEquals(HospitalOrder.Status.CANCELLED, orders.findByAdmissionId(1).get(0).getStatus());
        assertEquals(0, orders.getMedicineStock("unknown"));
    }

    @Test
    void admissionRepositoryCancelsAndResets() {
        AdmissionRepository admissions = new AdmissionRepository(folder.resolve("hospital.db"));
        Admission admission = admissions.saveAdmission("Aibek", "INS-100");
        assertNull(admissions.findById(99));
        admissions.cancelAdmission(admission.getId());
        admissions.cancelAdmission(admission.getId());
        assertEquals(Admission.Status.CANCELLED, admissions.findById(admission.getId()).getStatus());
        admissions.reset();
        assertTrue(admissions.findAll().isEmpty());
    }
}
