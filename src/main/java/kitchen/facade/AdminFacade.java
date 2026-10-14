package kitchen.facade;

import kitchen.cooking.CookingLine;
import kitchen.cooking.Ticket;
import kitchen.delivery.Bill;
import kitchen.delivery.BillingModule;
import kitchen.intake.Order;
import kitchen.intake.OrderQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminFacade {
    private final OrderQueue queue;
    private final CookingLine cooking;
    private final BillingModule billing;
    private final ChainRepository chains;
    private final Compensator compensator;

    public AdminFacade(OrderQueue queue, CookingLine cooking, BillingModule billing, ChainRepository chains) {
        this.queue = queue;
        this.cooking = cooking;
        this.billing = billing;
        this.chains = chains;
        this.compensator = new Compensator(queue, cooking, billing, chains);
    }

    public List<String> report(int orderId) {
        List<String> lines = new ArrayList<>();
        ChainRecord record = chains.find(orderId);
        if (record == null) {
            lines.add("No chain for order " + orderId);
            return lines;
        }
        lines.add("Chain: " + record.getStatus());
        Order order = queue.find(orderId);
        lines.add("A: " + (order == null ? "nothing" : order.toString()));
        Ticket ticket = cooking.find(orderId);
        lines.add("B: " + (ticket == null ? "nothing" : ticket.toString()));
        Bill bill = billing.find(orderId);
        lines.add("C: " + (bill == null ? "nothing" : bill.toString()));
        return lines;
    }

    public List<String> listChains() {
        List<String> lines = new ArrayList<>();
        for (ChainRecord record : chains.all()) {
            lines.add("Order #" + record.getOrderId() + " -> " + record.getStatus());
        }
        return lines;
    }

    public int countPending() {
        int count = 0;
        for (ChainRecord record : chains.all()) {
            if (record.getStatus() == ChainStatus.PENDING_B || record.getStatus() == ChainStatus.PENDING_C) {
                count++;
            }
        }
        return count;
    }

    public List<String> compensate(int orderId) {
        return compensator.compensate(orderId);
    }

    public int recoverPending() {
        int count = 0;
        for (ChainRecord record : chains.all()) {
            if (record.getStatus() == ChainStatus.PENDING_B || record.getStatus() == ChainStatus.PENDING_C) {
                compensator.compensate(record.getOrderId());
                count++;
            }
        }
        return count;
    }

    public Map<String, Integer> stock() {
        return queue.stock();
    }

    public void restock(String ingredient, int amount) {
        queue.restock(ingredient.trim().toLowerCase(), amount);
    }

    public int simulateCrashAfterB(String dish, int quantity, int quality) {
        int id = queue.accept(dish, quantity);
        chains.setStatus(id, ChainStatus.PENDING_B);
        cooking.start(id, dish, quality);
        chains.setStatus(id, ChainStatus.PENDING_C);
        return id;
    }
}
