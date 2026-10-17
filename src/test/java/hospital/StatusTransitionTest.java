package hospital;

import bed.BedAssignment;
import orders.HospitalOrder;
import org.junit.jupiter.api.Test;
import registration.Admission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StatusTransitionTest {
    @Test
    void admissionAllowsOnlyNewRegisteredCancelled() {
        Admission admission = new Admission(1, "Aibek", "INS-100", Admission.Status.NEW);
        assertThrows(IllegalStateException.class, admission::cancel);
        admission.register();
        assertEquals(Admission.Status.REGISTERED, admission.getStatus());
        assertThrows(IllegalStateException.class, admission::register);
        admission.cancel();
        assertEquals(Admission.Status.CANCELLED, admission.getStatus());
        assertThrows(IllegalStateException.class, admission::cancel);
    }

    @Test
    void bedAllowsAssignmentAndOneRelease() {
        BedAssignment bed = new BedAssignment(1, "GENERAL", "BASIC", BedAssignment.Status.RESERVED);
        bed.assign();
        assertEquals(BedAssignment.Status.ASSIGNED, bed.getStatus());
        assertThrows(IllegalStateException.class, bed::assign);
        bed.release();
        assertEquals(BedAssignment.Status.RELEASED, bed.getStatus());
        assertThrows(IllegalStateException.class, bed::release);
    }

    @Test
    void orderAllowsSendAndOneCancellation() {
        HospitalOrder order = new HospitalOrder(1, 1, "LAB", "", HospitalOrder.Status.CREATED);
        order.send();
        assertEquals(HospitalOrder.Status.SENT, order.getStatus());
        assertThrows(IllegalStateException.class, order::send);
        order.cancel();
        assertEquals(HospitalOrder.Status.CANCELLED, order.getStatus());
        assertThrows(IllegalStateException.class, order::cancel);
    }
}
