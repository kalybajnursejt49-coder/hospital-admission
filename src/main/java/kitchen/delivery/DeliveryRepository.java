package kitchen.delivery;

import kitchen.storage.FileStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DeliveryRepository {
    private final FileStore file;

    public DeliveryRepository(Path dir) {
        file = new FileStore(dir.resolve("bills.csv"));
    }

    public List<Bill> load() {
        List<Bill> bills = new ArrayList<>();
        for (String line : file.readLines()) {
            if (!line.isBlank()) {
                bills.add(Bill.fromLine(line));
            }
        }
        return bills;
    }

    public void save(List<Bill> bills) {
        List<String> lines = new ArrayList<>();
        for (Bill bill : bills) {
            lines.add(bill.toLine());
        }
        file.writeLines(lines);
    }

    public Bill find(int orderId) {
        for (Bill bill : load()) {
            if (bill.getOrderId() == orderId) {
                return bill;
            }
        }
        return null;
    }
}
