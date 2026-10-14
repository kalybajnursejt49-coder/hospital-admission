package kitchen.facade;

import kitchen.cooking.CookingException;
import kitchen.cooking.CookingLine;
import kitchen.delivery.BillingModule;
import kitchen.delivery.DeliveryDispatcher;
import kitchen.delivery.DeliveryException;
import kitchen.intake.IntakeException;
import kitchen.intake.OrderQueue;

public class OrderFacade {
    private final OrderQueue queue;
    private final CookingLine cooking;
    private final BillingModule billing;
    private final DeliveryDispatcher dispatcher;
    private final ChainRepository chains;
    private final Compensator compensator;

    public OrderFacade(OrderQueue queue, CookingLine cooking, BillingModule billing,
                       DeliveryDispatcher dispatcher, ChainRepository chains) {
        this.queue = queue;
        this.cooking = cooking;
        this.billing = billing;
        this.dispatcher = dispatcher;
        this.chains = chains;
        this.compensator = new Compensator(queue, cooking, billing, chains);
    }

    public int placeOrder(String dish, int quantity, int quality, String zone) {
        if (dish == null || dish.isBlank()) {
            throw new InvalidRequestException("Dish is required");
        }
        if (zone == null || zone.isBlank()) {
            throw new InvalidRequestException("Zone is required");
        }
        if (quantity <= 0) {
            throw new InvalidRequestException("Quantity must be positive");
        }
        if (quality < 0 || quality > 100) {
            throw new InvalidRequestException("Quality must be between 0 and 100");
        }
        String cleanDish = dish.trim().toLowerCase();
        String cleanZone = zone.trim().toUpperCase();

        int id;
        try {
            id = queue.accept(cleanDish, quantity);
        } catch (IntakeException e) {
            throw new OrderFailedException("A", -1, e.getMessage());
        }
        chains.setStatus(id, ChainStatus.PENDING_B);

        try {
            cooking.start(id, cleanDish, quality);
        } catch (CookingException e) {
            compensator.compensate(id);
            throw new OrderFailedException("B", id, e.getMessage());
        }
        chains.setStatus(id, ChainStatus.PENDING_C);

        try {
            billing.issue(id, cleanDish, quantity);
            dispatcher.dispatch(id, cleanZone);
            billing.pay(id);
        } catch (DeliveryException e) {
            compensator.compensate(id);
            throw new OrderFailedException("C", id, e.getMessage());
        }
        queue.complete(id);
        chains.setStatus(id, ChainStatus.COMPLETED);
        return id;
    }
}
