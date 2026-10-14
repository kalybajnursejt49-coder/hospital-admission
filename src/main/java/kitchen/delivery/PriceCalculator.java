package kitchen.delivery;

import java.util.HashMap;
import java.util.Map;

public class PriceCalculator {
    public static final int DELIVERY_FEE = 500;
    private final Map<String, Integer> prices = new HashMap<>();

    public PriceCalculator() {
        prices.put("burger", 2500);
        prices.put("pizza", 4000);
        prices.put("salad", 1800);
        prices.put("fries", 1200);
    }

    public int total(String dish, int quantity) {
        Integer price = prices.get(dish);
        if (price == null) {
            throw new DeliveryException("No price for " + dish);
        }
        return price * quantity + DELIVERY_FEE;
    }
}
