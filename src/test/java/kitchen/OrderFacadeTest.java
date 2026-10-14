package kitchen;

import kitchen.cooking.TicketStatus;
import kitchen.delivery.BillStatus;
import kitchen.facade.ChainStatus;
import kitchen.facade.InvalidRequestException;
import kitchen.facade.OrderFailedException;
import kitchen.intake.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderFacadeTest {
    @TempDir
    Path dir;
    KitchenTestKit kitchen;

    @BeforeEach
    void setUp() {
        kitchen = new KitchenTestKit(dir);
    }

    @Test
    void happyPathCompletesWholeChain() {
        int id = kitchen.customerOrders("burger", 2);
        kitchen.assertChainIs(id, ChainStatus.COMPLETED);
        kitchen.assertOrderIs(id, OrderStatus.COMPLETED);
        kitchen.assertTicketIs(id, TicketStatus.COOKED);
        kitchen.assertBillIs(id, BillStatus.PAID);
        kitchen.assertStock("bun", 8);
    }

    @Test
    void failureInAStopsBeforeAnythingIsCommitted() {
        int id = kitchen.orderFailsAtStage("A", "burger", 50, 90, "CENTER");
        assertEquals(-1, id);
        assertTrue(kitchen.app().admin().listChains().isEmpty());
        kitchen.assertStock("bun", 10);
    }

    @Test
    void unknownDishFailsInA() {
        kitchen.orderFailsAtStage("A", "sushi", 1, 90, "CENTER");
        assertTrue(kitchen.app().admin().listChains().isEmpty());
    }

    @Test
    void failureInBCompensatesOnlyA() {
        int id = kitchen.chefBurnsTheBurger();
        kitchen.assertChainIs(id, ChainStatus.COMPENSATED);
        kitchen.assertOrderIs(id, OrderStatus.CANCELLED);
        kitchen.assertTicketIs(id, TicketStatus.DISCARDED);
        kitchen.assertNoBillFor(id);
        kitchen.assertStock("bun", 10);
        kitchen.assertStock("patty", 10);
    }

    @Test
    void failureInCCompensatesBThenA() {
        int id = kitchen.courierCannotReachCustomer();
        kitchen.assertChainIs(id, ChainStatus.COMPENSATED);
        kitchen.assertBillIs(id, BillStatus.VOIDED);
        kitchen.assertTicketIs(id, TicketStatus.DISCARDED);
        kitchen.assertOrderIs(id, OrderStatus.CANCELLED);
        kitchen.assertStock("bun", 10);
    }

    @Test
    void failedOrdersDoNotBlockNextOrders() {
        kitchen.chefBurnsTheBurger();
        kitchen.courierCannotReachCustomer();
        int id = kitchen.customerOrders("burger", 1);
        kitchen.assertChainIs(id, ChainStatus.COMPLETED);
        kitchen.assertStock("bun", 9);
    }

    @Test
    void invalidInputIsRejectedBeforeAnyStep() {
        assertThrows(InvalidRequestException.class, () -> kitchen.app().orders().placeOrder("", 1, 90, "CENTER"));
        assertThrows(InvalidRequestException.class, () -> kitchen.app().orders().placeOrder(null, 1, 90, "CENTER"));
        assertThrows(InvalidRequestException.class, () -> kitchen.app().orders().placeOrder("burger", 1, 90, " "));
        assertThrows(InvalidRequestException.class, () -> kitchen.app().orders().placeOrder("burger", 1, 90, null));
        assertThrows(InvalidRequestException.class, () -> kitchen.app().orders().placeOrder("burger", 0, 90, "CENTER"));
        assertThrows(InvalidRequestException.class, () -> kitchen.app().orders().placeOrder("burger", 1, 101, "CENTER"));
        assertThrows(InvalidRequestException.class, () -> kitchen.app().orders().placeOrder("burger", 1, -1, "CENTER"));
        assertTrue(kitchen.app().admin().listChains().isEmpty());
    }

    @Test
    void inputIsNormalized() {
        int id = kitchen.app().orders().placeOrder("  Burger ", 1, 90, " center ");
        kitchen.assertOrderIs(id, OrderStatus.COMPLETED);
    }

    @Test
    void failureMessageNamesTheStage() {
        OrderFailedException e = assertThrows(OrderFailedException.class,
                () -> kitchen.app().orders().placeOrder("burger", 1, 10, "CENTER"));
        assertTrue(e.getMessage().startsWith("Stage B"));
        assertEquals(1, e.getOrderId());
    }

    @Test
    void completedStateSurvivesRestart() {
        int id = kitchen.customerOrders("pizza", 1);
        kitchen.restartKitchen();
        kitchen.assertChainIs(id, ChainStatus.COMPLETED);
        kitchen.assertOrderIs(id, OrderStatus.COMPLETED);
        kitchen.assertBillIs(id, BillStatus.PAID);
        kitchen.assertStock("dough", 9);
        int next = kitchen.customerOrders("pizza", 1);
        assertEquals(id + 1, next);
    }
}
