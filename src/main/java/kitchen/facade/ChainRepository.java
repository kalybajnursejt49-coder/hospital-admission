package kitchen.facade;

import kitchen.storage.FileStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ChainRepository {
    private final FileStore file;

    public ChainRepository(Path dir) {
        file = new FileStore(dir.resolve("chains.csv"));
    }

    public List<ChainRecord> all() {
        List<ChainRecord> records = new ArrayList<>();
        for (String line : file.readLines()) {
            if (!line.isBlank()) {
                records.add(ChainRecord.fromLine(line));
            }
        }
        return records;
    }

    public ChainRecord find(int orderId) {
        for (ChainRecord record : all()) {
            if (record.getOrderId() == orderId) {
                return record;
            }
        }
        return null;
    }

    public void setStatus(int orderId, ChainStatus status) {
        List<ChainRecord> records = all();
        boolean found = false;
        for (ChainRecord record : records) {
            if (record.getOrderId() == orderId) {
                record.setStatus(status);
                found = true;
            }
        }
        if (!found) {
            records.add(new ChainRecord(orderId, status));
        }
        List<String> lines = new ArrayList<>();
        for (ChainRecord record : records) {
            lines.add(record.toLine());
        }
        file.writeLines(lines);
    }
}
