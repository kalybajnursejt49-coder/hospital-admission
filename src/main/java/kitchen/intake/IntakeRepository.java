package kitchen.intake;

import kitchen.storage.FileStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class IntakeRepository {
    private final FileStore orderFile;
    private final FileStore stockFile;

    public IntakeRepository(Path dir) {
        orderFile = new FileStore(dir.resolve("orders.csv"));
        stockFile = new FileStore(dir.resolve("stock.csv"));
        if (!stockFile.exists()) {
            Map<String, Integer> start = new TreeMap<>();
            start.put("bun", 10);
            start.put("patty", 10);
            start.put("dough", 10);
            start.put("cheese", 20);
            start.put("lettuce", 10);
            start.put("tomato", 10);
            start.put("potato", 30);
            saveStock(start);
        }
    }

    public List<Order> loadOrders() {
        List<Order> orders = new ArrayList<>();
        for (String line : orderFile.readLines()) {
            if (!line.isBlank()) {
                orders.add(Order.fromLine(line));
            }
        }
        return orders;
    }

    public void saveOrders(List<Order> orders) {
        List<String> lines = new ArrayList<>();
        for (Order order : orders) {
            lines.add(order.toLine());
        }
        orderFile.writeLines(lines);
    }

    public Map<String, Integer> loadStock() {
        Map<String, Integer> stock = new TreeMap<>();
        for (String line : stockFile.readLines()) {
            if (!line.isBlank()) {
                String[] p = line.split(";");
                stock.put(p[0], Integer.parseInt(p[1]));
            }
        }
        return stock;
    }

    public void saveStock(Map<String, Integer> stock) {
        List<String> lines = new ArrayList<>();
        for (String ingredient : stock.keySet()) {
            lines.add(ingredient + ";" + stock.get(ingredient));
        }
        stockFile.writeLines(lines);
    }
}
