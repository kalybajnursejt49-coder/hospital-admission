package kitchen.intake;

public class Order {
    private final int id;
    private final String dish;
    private final int quantity;
    private OrderStatus status;

    public Order(int id, String dish, int quantity, OrderStatus status) {
        this.id = id;
        this.dish = dish;
        this.quantity = quantity;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getDish() {
        return dish;
    }

    public int getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void changeStatus(OrderStatus next) {
        boolean legal = false;
        if (status == OrderStatus.QUEUED && (next == OrderStatus.COMPLETED || next == OrderStatus.CANCELLED)) {
            legal = true;
        }
        if (status == OrderStatus.COMPLETED && next == OrderStatus.CANCELLED) {
            legal = true;
        }
        if (!legal) {
            throw new IntakeException("Order " + id + " cannot go from " + status + " to " + next);
        }
        status = next;
    }

    public String toLine() {
        return id + ";" + dish + ";" + quantity + ";" + status;
    }

    public static Order fromLine(String line) {
        String[] p = line.split(";");
        return new Order(Integer.parseInt(p[0]), p[1], Integer.parseInt(p[2]), OrderStatus.valueOf(p[3]));
    }

    @Override
    public String toString() {
        return "Order #" + id + " " + quantity + " x " + dish + " [" + status + "]";
    }
}
