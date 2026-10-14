package kitchen.cooking;

import java.util.List;

public class CookingLine {
    private final CookingRepository repository;
    private final StationAllocator allocator;
    private final QualityChecker checker;

    public CookingLine(CookingRepository repository, StationAllocator allocator, QualityChecker checker) {
        this.repository = repository;
        this.allocator = allocator;
        this.checker = checker;
    }

    public Ticket start(int orderId, String dish, int quality) {
        List<Ticket> tickets = repository.load();
        if (findIn(tickets, orderId) != null) {
            throw new CookingException("Order " + orderId + " already has a ticket");
        }
        String station = allocator.allocate(dish, tickets);
        Ticket ticket = new Ticket(orderId, dish, station, TicketStatus.ASSIGNED);
        tickets.add(ticket);
        repository.save(tickets);
        if (!checker.passes(quality)) {
            ticket.changeStatus(TicketStatus.DISCARDED);
            repository.save(tickets);
            throw new CookingException("Quality check failed with score " + quality);
        }
        ticket.changeStatus(TicketStatus.COOKED);
        repository.save(tickets);
        return ticket;
    }

    public void discard(int orderId) {
        List<Ticket> tickets = repository.load();
        Ticket ticket = findIn(tickets, orderId);
        if (ticket == null) {
            throw new CookingException("No ticket for order " + orderId);
        }
        ticket.changeStatus(TicketStatus.DISCARDED);
        repository.save(tickets);
    }

    public Ticket find(int orderId) {
        return findIn(repository.load(), orderId);
    }

    public List<Ticket> all() {
        return repository.load();
    }

    private Ticket findIn(List<Ticket> tickets, int orderId) {
        for (Ticket ticket : tickets) {
            if (ticket.getOrderId() == orderId) {
                return ticket;
            }
        }
        return null;
    }
}
