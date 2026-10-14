package kitchen;

import kitchen.delivery.Bill;
import kitchen.delivery.BillStatus;
import kitchen.delivery.BillingModule;
import kitchen.delivery.DeliveryDispatcher;
import kitchen.delivery.DeliveryException;
import kitchen.delivery.DeliveryRepository;
import kitchen.delivery.PriceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryTest {
    @TempDir
    Path dir;
    BillingModule billing;
    DeliveryDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        DeliveryRepository repository = new DeliveryRepository(dir);
        billing = new BillingModule(repository, new PriceCalculator());
        dispatcher = new DeliveryDispatcher(repository);
    }

    @Test
    void billAmountIncludesDeliveryFee() {
        Bill bill = billing.issue(1, "burger", 2);
        assertEquals(2 * 2500 + 500, bill.getAmount());
        assertEquals(BillStatus.ISSUED, billing.find(1).getStatus());
    }

    @Test
    void unknownDishHasNoPrice() {
        assertThrows(DeliveryException.class, () -> billing.issue(1, "sushi", 1));
    }

    @Test
    void secondBillForSameOrderIsRejected() {
        billing.issue(1, "burger", 1);
        assertThrows(DeliveryException.class, () -> billing.issue(1, "burger", 1));
    }

    @Test
    void payAndVoid() {
        billing.issue(1, "pizza", 1);
        billing.pay(1);
        assertEquals(BillStatus.PAID, billing.find(1).getStatus());
        billing.voidBill(1);
        assertEquals(BillStatus.VOIDED, billing.find(1).getStatus());
    }

    @Test
    void voidedBillCannotBePaid() {
        billing.issue(1, "pizza", 1);
        billing.voidBill(1);
        assertThrows(DeliveryException.class, () -> billing.pay(1));
    }

    @Test
    void missingBillThrows() {
        assertThrows(DeliveryException.class, () -> billing.pay(9));
        assertThrows(DeliveryException.class, () -> dispatcher.dispatch(9, "CENTER"));
        assertNull(billing.find(9));
    }

    @Test
    void dispatchAssignsCourierAndZone() {
        billing.issue(4, "fries", 1);
        String courier = dispatcher.dispatch(4, "NORTH");
        assertEquals(courier, billing.find(4).getCourier());
        assertEquals("NORTH", billing.find(4).getZone());
    }

    @Test
    void unknownZoneIsRejected() {
        billing.issue(4, "fries", 1);
        assertThrows(DeliveryException.class, () -> dispatcher.dispatch(4, "MARS"));
        assertEquals("-", billing.find(4).getCourier());
    }

    @Test
    void allBillsAreListed() {
        billing.issue(1, "fries", 1);
        billing.issue(2, "salad", 1);
        assertEquals(2, billing.all().size());
        assertEquals(3, dispatcher.zones().size());
    }

    @Test
    void billLineRoundTrip() {
        Bill bill = new Bill(2, 3000, "SOUTH", "Dias", BillStatus.PAID);
        Bill copy = Bill.fromLine(bill.toLine());
        assertEquals(3000, copy.getAmount());
        assertEquals("Dias", copy.getCourier());
        assertTrue(copy.toString().contains("SOUTH"));
        assertEquals(2, copy.getOrderId());
    }
}
