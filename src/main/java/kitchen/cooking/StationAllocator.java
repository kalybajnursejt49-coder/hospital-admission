package kitchen.cooking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StationAllocator {
    private final Map<String, String> stationByDish = new HashMap<>();

    public StationAllocator() {
        stationByDish.put("burger", "GRILL");
        stationByDish.put("fries", "FRYER");
        stationByDish.put("pizza", "OVEN");
        stationByDish.put("salad", "COLD");
    }

    public String allocate(String dish, List<Ticket> tickets) {
        String station = stationByDish.get(dish);
        if (station == null) {
            throw new CookingException("No station can cook " + dish);
        }
        for (Ticket ticket : tickets) {
            if (ticket.getStation().equals(station) && ticket.getStatus() == TicketStatus.ASSIGNED) {
                throw new CookingException("Station " + station + " is busy");
            }
        }
        return station;
    }
}
