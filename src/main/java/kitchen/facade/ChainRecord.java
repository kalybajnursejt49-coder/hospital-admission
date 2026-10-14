package kitchen.facade;

public class ChainRecord {
    private final int orderId;
    private ChainStatus status;

    public ChainRecord(int orderId, ChainStatus status) {
        this.orderId = orderId;
        this.status = status;
    }

    public int getOrderId() {
        return orderId;
    }

    public ChainStatus getStatus() {
        return status;
    }

    public void setStatus(ChainStatus status) {
        this.status = status;
    }

    public String toLine() {
        return orderId + ";" + status;
    }

    public static ChainRecord fromLine(String line) {
        String[] p = line.split(";");
        return new ChainRecord(Integer.parseInt(p[0]), ChainStatus.valueOf(p[1]));
    }
}
