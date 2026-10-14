package kitchen;

import kitchen.intake.IntakeException;
import kitchen.intake.IntakeRepository;
import kitchen.intake.InventoryChecker;
import kitchen.intake.Order;
import kitchen.intake.OrderQueue;
import kitchen.intake.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntakeTest {
    @TempDir
    Path dir;
    OrderQueue queue;
    InventoryChecker checker;

    @BeforeEach
    void setUp() {
        checker = new InventoryChecker();
        queue = new OrderQueue(new IntakeRepository(dir), checker);
    }

    @Test
    void acceptingOrderReducesStock() {
        int id = queue.accept("burger", 2);
        assertEquals(1, id);
        assertEquals(8, queue.stock().get("bun"));
        assertEquals(8, queue.stock().get("patty"));
        assertEquals(OrderStatus.QUEUED, queue.find(id).getStatus());
    }

    @Test
    void idsGrowOneByOne() {
        queue.accept("burger", 1);
        assertEquals(2, queue.accept("fries", 1));
    }

    @Test
    void unknownDishIsRejected() {
        assertThrows(IntakeException.class, () -> queue.accept("sushi", 1));
    }

    @Test
    void zeroQuantityIsRejected() {
        assertThrows(IntakeException.class, () -> queue.accept("burger", 0));
    }

    @Test
    void notEnoughStockIsRejectedAndNothingChanges() {
        assertThrows(IntakeException.class, () -> queue.accept("burger", 11));
        assertEquals(10, queue.stock().get("bun"));
        assertTrue(queue.all().isEmpty());
    }

    @Test
    void cancelRestoresStock() {
        int id = queue.accept("pizza", 3);
        queue.cancel(id);
        assertEquals(10, queue.stock().get("dough"));
        assertEquals(20, queue.stock().get("cheese"));
        assertEquals(OrderStatus.CANCELLED, queue.find(id).getStatus());
    }

    @Test
    void cancelledOrderCannotBeCompleted() {
        int id = queue.accept("salad", 1);
        queue.cancel(id);
        assertThrows(IntakeException.class, () -> queue.complete(id));
    }

    @Test
    void completedOrderCanStillBeCancelled() {
        int id = queue.accept("salad", 1);
        queue.complete(id);
        queue.cancel(id);
        assertEquals(OrderStatus.CANCELLED, queue.find(id).getStatus());
    }

    @Test
    void cancelledOrderCannotBeCancelledTwice() {
        int id = queue.accept("salad", 1);
        queue.cancel(id);
        assertThrows(IntakeException.class, () -> queue.cancel(id));
    }

    @Test
    void missingOrderThrows() {
        assertThrows(IntakeException.class, () -> queue.cancel(99));
        assertThrows(IntakeException.class, () -> queue.complete(99));
        assertNull(queue.find(99));
    }

    @Test
    void restockAddsIngredients() {
        queue.restock("bun", 5);
        assertEquals(15, queue.stock().get("bun"));
        assertThrows(IntakeException.class, () -> queue.restock("bun", 0));
    }

    @Test
    void stateSurvivesNewObjects() {
        queue.accept("burger", 1);
        OrderQueue again = new OrderQueue(new IntakeRepository(dir), new InventoryChecker());
        assertEquals(1, again.all().size());
        assertEquals(9, again.stock().get("bun"));
    }

    @Test
    void checkerCalculatesNeededIngredients() {
        Map<String, Integer> need = checker.needed("pizza", 2);
        assertEquals(2, need.get("dough"));
        assertEquals(4, need.get("cheese"));
        assertEquals(4, checker.dishes().size());
    }

    @Test
    void orderLineRoundTrip() {
        Order order = new Order(5, "fries", 2, OrderStatus.QUEUED);
        Order copy = Order.fromLine(order.toLine());
        assertEquals(5, copy.getId());
        assertEquals("fries", copy.getDish());
        assertEquals(2, copy.getQuantity());
        assertTrue(copy.toString().contains("fries"));
    }
}
