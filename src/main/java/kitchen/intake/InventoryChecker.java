package kitchen.intake;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InventoryChecker {
    private final Map<String, Map<String, Integer>> recipes = new HashMap<>();

    public InventoryChecker() {
        Map<String, Integer> burger = new HashMap<>();
        burger.put("bun", 1);
        burger.put("patty", 1);
        recipes.put("burger", burger);

        Map<String, Integer> pizza = new HashMap<>();
        pizza.put("dough", 1);
        pizza.put("cheese", 2);
        recipes.put("pizza", pizza);

        Map<String, Integer> salad = new HashMap<>();
        salad.put("lettuce", 2);
        salad.put("tomato", 1);
        recipes.put("salad", salad);

        Map<String, Integer> fries = new HashMap<>();
        fries.put("potato", 3);
        recipes.put("fries", fries);
    }

    public boolean isKnownDish(String dish) {
        return recipes.containsKey(dish);
    }

    public Set<String> dishes() {
        return recipes.keySet();
    }

    public Map<String, Integer> needed(String dish, int quantity) {
        Map<String, Integer> result = new HashMap<>();
        Map<String, Integer> recipe = recipes.get(dish);
        for (String ingredient : recipe.keySet()) {
            result.put(ingredient, recipe.get(ingredient) * quantity);
        }
        return result;
    }

    public String firstMissing(Map<String, Integer> stock, String dish, int quantity) {
        Map<String, Integer> need = needed(dish, quantity);
        for (String ingredient : need.keySet()) {
            int have = stock.getOrDefault(ingredient, 0);
            if (have < need.get(ingredient)) {
                return ingredient;
            }
        }
        return null;
    }
}
