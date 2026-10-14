package kitchen.delivery;

public class Bill {
    private final int orderId;
    private final int amount;
    private String zone;
    private String courier;
    private BillStatus status;

    public Bill(int orderId, int amount, String zone, String courier, BillStatus status) {
        this.orderId = orderId;
        this.amount = amount;
        this.zone = zone;
        this.courier = courier;
        this.status = status;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getAmount() {
        return amount;
    }

    public String getZone() {
        return zone;
    }

    public String getCourier() {
        return courier;
    }

    public BillStatus getStatus() {
        return status;
    }

    public void assign(String zone, String courier) {
        this.zone = zone;
        this.courier = courier;
    }

    public void changeStatus(BillStatus next) {
        boolean legal = false;
        if (status == BillStatus.ISSUED && (next == BillStatus.PAID || next == BillStatus.VOIDED)) {
            legal = true;
        }
        if (status == BillStatus.PAID && next == BillStatus.VOIDED) {
            legal = true;
        }
        if (!legal) {
            throw new DeliveryException("Bill " + orderId + " cannot go from " + status + " to " + next);
        }
        status = next;
    }

    public String toLine() {
        return orderId + ";" + amount + ";" + zone + ";" + courier + ";" + status;
    }

    public static Bill fromLine(String line) {
        String[] p = line.split(";");
        return new Bill(Integer.parseInt(p[0]), Integer.parseInt(p[1]), p[2], p[3], BillStatus.valueOf(p[4]));
    }

    @Override
    public String toString() {
        return "Bill for order #" + orderId + " " + amount + " KZT, zone " + zone + ", courier " + courier + " [" + status + "]";
    }
}
