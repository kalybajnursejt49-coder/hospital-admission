package kitchen;

import kitchen.cooking.Ticket;
import kitchen.cooking.TicketStatus;
import kitchen.delivery.Bill;
import kitchen.delivery.BillStatus;
import kitchen.facade.ChainStatus;
import kitchen.facade.OrderFailedException;
import kitchen.intake.Order;
import kitchen.intake.OrderStatus;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class KitchenTestKit {
    private final Path dir;
    private KitchenApp app;

    public KitchenTestKit(Path dir) {
        this.dir = dir;
        this.app = new KitchenApp(dir);
    }

    public KitchenApp app() {
        return app;
    }

    public void restartKitchen() {
        app = new KitchenApp(dir);
    }

    public int customerOrders(String dish, int quantity) {
        return app.orders().placeOrder(dish, quantity, 90, "CENTER");
    }

    public int chefBurnsTheBurger() {
        return orderFailsAtStage("B", "burger", 1, 30, "CENTER");
    }

    public int courierCannotReachCustomer() {
        return orderFailsAtStage("C", "burger", 1, 90, "MARS");
    }

    public int orderFailsAtStage(String stage, String dish, int quantity, int quality, String zone) {
        try {
            app.orders().placeOrder(dish, quantity, quality, zone);
        } catch (OrderFailedException e) {
            assertEquals(stage, e.getStage());
            return e.getOrderId();
        }
        fail("Order should have failed at stage " + stage);
        return -1;
    }

    public void assertChainIs(int orderId, ChainStatus expected) {
        assertEquals(expected, findChain(orderId));
    }

    private ChainStatus findChain(int orderId) {
        for (String line : app.admin().listChains()) {
            if (line.startsWith("Order #" + orderId + " ")) {
                return ChainStatus.valueOf(line.substring(line.indexOf("-> ") + 3));
            }
        }
        return null;
    }

    public void assertOrderIs(int orderId, OrderStatus expected) {
        Order order = findOrder(orderId);
        assertNotNull(order);
        assertEquals(expected, order.getStatus());
    }

    public void assertTicketIs(int orderId, TicketStatus expected) {
        Ticket ticket = findTicket(orderId);
        assertNotNull(ticket);
        assertEquals(expected, ticket.getStatus());
    }

    public void assertNoTicketFor(int orderId) {
        assertNull(findTicket(orderId));
    }

    public void assertBillIs(int orderId, BillStatus expected) {
        Bill bill = findBill(orderId);
        assertNotNull(bill);
        assertEquals(expected, bill.getStatus());
    }

    public void assertNoBillFor(int orderId) {
        assertNull(findBill(orderId));
    }

    public void assertStock(String ingredient, int expected) {
        assertEquals(expected, app.admin().stock().get(ingredient));
    }

    private Order findOrder(int orderId) {
        return new kitchen.intake.IntakeRepository(dir).loadOrders().stream()
                .filter(o -> o.getId() == orderId).findFirst().orElse(null);
    }

    private Ticket findTicket(int orderId) {
        return new kitchen.cooking.CookingRepository(dir).load().stream()
                .filter(t -> t.getOrderId() == orderId).findFirst().orElse(null);
    }

    private Bill findBill(int orderId) {
        return new kitchen.delivery.DeliveryRepository(dir).find(orderId);
    }
}
