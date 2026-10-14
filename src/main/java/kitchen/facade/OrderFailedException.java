package kitchen.facade;

public class OrderFailedException extends RuntimeException {
    private final String stage;
    private final int orderId;

    public OrderFailedException(String stage, int orderId, String message) {
        super("Stage " + stage + " failed: " + message);
        this.stage = stage;
        this.orderId = orderId;
    }

    public String getStage() {
        return stage;
    }

    public int getOrderId() {
        return orderId;
    }
}
