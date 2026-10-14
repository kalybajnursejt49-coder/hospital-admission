package kitchen.cooking;

public class Ticket {
    private final int orderId;
    private final String dish;
    private final String station;
    private TicketStatus status;

    public Ticket(int orderId, String dish, String station, TicketStatus status) {
        this.orderId = orderId;
        this.dish = dish;
        this.station = station;
        this.status = status;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getDish() {
        return dish;
    }

    public String getStation() {
        return station;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void changeStatus(TicketStatus next) {
        boolean legal = false;
        if (status == TicketStatus.ASSIGNED && (next == TicketStatus.COOKED || next == TicketStatus.DISCARDED)) {
            legal = true;
        }
        if (status == TicketStatus.COOKED && next == TicketStatus.DISCARDED) {
            legal = true;
        }
        if (!legal) {
            throw new CookingException("Ticket " + orderId + " cannot go from " + status + " to " + next);
        }
        status = next;
    }

    public String toLine() {
        return orderId + ";" + dish + ";" + station + ";" + status;
    }

    public static Ticket fromLine(String line) {
        String[] p = line.split(";");
        return new Ticket(Integer.parseInt(p[0]), p[1], p[2], TicketStatus.valueOf(p[3]));
    }

    @Override
    public String toString() {
        return "Ticket for order #" + orderId + " " + dish + " at " + station + " [" + status + "]";
    }
}
