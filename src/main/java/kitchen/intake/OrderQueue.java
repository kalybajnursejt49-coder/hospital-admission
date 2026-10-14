package kitchen.intake;

import java.util.List;
import java.util.Map;

public class OrderQueue {
    private final IntakeRepository repository;
    private final InventoryChecker checker;

    public OrderQueue(IntakeRepository repository, InventoryChecker checker) {
        this.repository = repository;
        this.checker = checker;
    }

    public int accept(String dish, int quantity) {
        if (!checker.isKnownDish(dish)) {
            throw new IntakeException("Unknown dish: " + dish);
        }
        if (quantity <= 0) {
            throw new IntakeException("Quantity must be positive");
        }
        Map<String, Integer> stock = repository.loadStock();
        String missing = checker.firstMissing(stock, dish, quantity);
        if (missing != null) {
            throw new IntakeException("Not enough " + missing + " in stock");
        }
        Map<String, Integer> need = checker.needed(dish, quantity);
        for (String ingredient : need.keySet()) {
            stock.put(ingredient, stock.get(ingredient) - need.get(ingredient));
        }
        repository.saveStock(stock);
        List<Order> orders = repository.loadOrders();
        int id = nextId(orders);
        orders.add(new Order(id, dish, quantity, OrderStatus.QUEUED));
        repository.saveOrders(orders);
        return id;
    }

    public void complete(int id) {
        List<Order> orders = repository.loadOrders();
        Order order = findIn(orders, id);
        if (order == null) {
            throw new IntakeException("Order " + id + " not found");
        }
        order.changeStatus(OrderStatus.COMPLETED);
        repository.saveOrders(orders);
    }

    public void cancel(int id) {
        List<Order> orders = repository.loadOrders();
        Order order = findIn(orders, id);
        if (order == null) {
            throw new IntakeException("Order " + id + " not found");
        }
        order.changeStatus(OrderStatus.CANCELLED);
        Map<String, Integer> stock = repository.loadStock();
        Map<String, Integer> need = checker.needed(order.getDish(), order.getQuantity());
        for (String ingredient : need.keySet()) {
            stock.put(ingredient, stock.getOrDefault(ingredient, 0) + need.get(ingredient));
        }
        repository.saveStock(stock);
        repository.saveOrders(orders);
    }

    public Order find(int id) {
        return findIn(repository.loadOrders(), id);
    }

    public List<Order> all() {
        return repository.loadOrders();
    }

    public Map<String, Integer> stock() {
        return repository.loadStock();
    }

    public void restock(String ingredient, int amount) {
        if (amount <= 0) {
            throw new IntakeException("Amount must be positive");
        }
        Map<String, Integer> stock = repository.loadStock();
        stock.put(ingredient, stock.getOrDefault(ingredient, 0) + amount);
        repository.saveStock(stock);
    }

    private int nextId(List<Order> orders) {
        int max = 0;
        for (Order order : orders) {
            if (order.getId() > max) {
                max = order.getId();
            }
        }
        return max + 1;
    }

    private Order findIn(List<Order> orders, int id) {
        for (Order order : orders) {
            if (order.getId() == id) {
                return order;
            }
        }
        return null;
    }
}
