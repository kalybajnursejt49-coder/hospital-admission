package kitchen.facade;

import kitchen.cooking.CookingLine;
import kitchen.cooking.Ticket;
import kitchen.cooking.TicketStatus;
import kitchen.delivery.Bill;
import kitchen.delivery.BillStatus;
import kitchen.delivery.BillingModule;
import kitchen.intake.Order;
import kitchen.intake.OrderQueue;
import kitchen.intake.OrderStatus;

import java.util.ArrayList;
import java.util.List;

class Compensator {
    private final OrderQueue queue;
    private final CookingLine cooking;
    private final BillingModule billing;
    private final ChainRepository chains;

    Compensator(OrderQueue queue, CookingLine cooking, BillingModule billing, ChainRepository chains) {
        this.queue = queue;
        this.cooking = cooking;
        this.billing = billing;
        this.chains = chains;
    }

    List<String> compensate(int orderId) {
        ChainRecord record = chains.find(orderId);
        if (record == null) {
            throw new InvalidRequestException("No chain for order " + orderId);
        }
        if (record.getStatus() == ChainStatus.COMPLETED) {
            throw new InvalidRequestException("Order " + orderId + " is already completed");
        }
        List<String> steps = new ArrayList<>();

        Bill bill = billing.find(orderId);
        if (bill != null && bill.getStatus() != BillStatus.VOIDED) {
            billing.voidBill(orderId);
            steps.add("C: bill voided");
        }

        Ticket ticket = cooking.find(orderId);
        if (ticket != null && ticket.getStatus() != TicketStatus.DISCARDED) {
            cooking.discard(orderId);
            steps.add("B: ticket discarded");
        }

        Order order = queue.find(orderId);
        if (order != null && order.getStatus() != OrderStatus.CANCELLED) {
            queue.cancel(orderId);
            steps.add("A: order cancelled, stock restored");
        }

        chains.setStatus(orderId, ChainStatus.COMPENSATED);
        return steps;
    }
}
