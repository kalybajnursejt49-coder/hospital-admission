package kitchen.cooking;

import kitchen.storage.FileStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CookingRepository {
    private final FileStore file;

    public CookingRepository(Path dir) {
        file = new FileStore(dir.resolve("tickets.csv"));
    }

    public List<Ticket> load() {
        List<Ticket> tickets = new ArrayList<>();
        for (String line : file.readLines()) {
            if (!line.isBlank()) {
                tickets.add(Ticket.fromLine(line));
            }
        }
        return tickets;
    }

    public void save(List<Ticket> tickets) {
        List<String> lines = new ArrayList<>();
        for (Ticket ticket : tickets) {
            lines.add(ticket.toLine());
        }
        file.writeLines(lines);
    }
}
