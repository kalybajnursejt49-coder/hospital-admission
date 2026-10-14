package kitchen.delivery;

import java.util.Arrays;
import java.util.List;

public class DeliveryDispatcher {
    private final DeliveryRepository repository;
    private final List<String> zones = Arrays.asList("CENTER", "NORTH", "SOUTH");
    private final String[] couriers = {"Aigerim", "Dias", "Marat"};

    public DeliveryDispatcher(DeliveryRepository repository) {
        this.repository = repository;
    }

    public String dispatch(int orderId, String zone) {
        if (!zones.contains(zone)) {
            throw new DeliveryException("We do not deliver to zone " + zone);
        }
        List<Bill> bills = repository.load();
        for (Bill bill : bills) {
            if (bill.getOrderId() == orderId) {
                String courier = couriers[orderId % couriers.length];
                bill.assign(zone, courier);
                repository.save(bills);
                return courier;
            }
        }
        throw new DeliveryException("No bill for order " + orderId);
    }

    public List<String> zones() {
        return zones;
    }
}
