package kitchen;

import kitchen.cooking.CookingException;
import kitchen.cooking.CookingLine;
import kitchen.cooking.CookingRepository;
import kitchen.cooking.QualityChecker;
import kitchen.cooking.StationAllocator;
import kitchen.cooking.Ticket;
import kitchen.cooking.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookingTest {
    @TempDir
    Path dir;
    CookingLine line;

    @BeforeEach
    void setUp() {
        line = new CookingLine(new CookingRepository(dir), new StationAllocator(), new QualityChecker());
    }

    @Test
    void goodDishIsCookedAtRightStation() {
        Ticket ticket = line.start(1, "burger", 80);
        assertEquals("GRILL", ticket.getStation());
        assertEquals(TicketStatus.COOKED, line.find(1).getStatus());
    }

    @Test
    void badQualityDiscardsTicket() {
        assertThrows(CookingException.class, () -> line.start(1, "burger", 20));
        assertEquals(TicketStatus.DISCARDED, line.find(1).getStatus());
    }

    @Test
    void unknownDishHasNoStation() {
        assertThrows(CookingException.class, () -> line.start(1, "sushi", 80));
        assertNull(line.find(1));
    }

    @Test
    void sameOrderCannotGetTwoTickets() {
        line.start(1, "burger", 80);
        assertThrows(CookingException.class, () -> line.start(1, "burger", 80));
    }

    @Test
    void busyStationIsRejected() {
        CookingRepository repository = new CookingRepository(dir);
        java.util.List<Ticket> tickets = new java.util.ArrayList<>();
        tickets.add(new Ticket(7, "burger", "GRILL", TicketStatus.ASSIGNED));
        repository.save(tickets);
        assertThrows(CookingException.class, () -> line.start(8, "burger", 80));
        assertEquals(TicketStatus.COOKED, line.start(9, "pizza", 80).getStatus());
    }

    @Test
    void freeStationAfterCooking() {
        line.start(1, "burger", 80);
        assertEquals(TicketStatus.COOKED, line.start(2, "burger", 80).getStatus());
        assertEquals(2, line.all().size());
    }

    @Test
    void discardCookedTicket() {
        line.start(1, "salad", 80);
        line.discard(1);
        assertEquals(TicketStatus.DISCARDED, line.find(1).getStatus());
    }

    @Test
    void discardTwiceFails() {
        line.start(1, "salad", 80);
        line.discard(1);
        assertThrows(CookingException.class, () -> line.discard(1));
    }

    @Test
    void discardMissingTicketFails() {
        assertThrows(CookingException.class, () -> line.discard(5));
    }

    @Test
    void qualityBorder() {
        QualityChecker checker = new QualityChecker();
        assertTrue(checker.passes(60));
        assertFalse(checker.passes(59));
    }

    @Test
    void ticketLineRoundTrip() {
        Ticket ticket = new Ticket(3, "fries", "FRYER", TicketStatus.COOKED);
        Ticket copy = Ticket.fromLine(ticket.toLine());
        assertEquals(3, copy.getOrderId());
        assertEquals("fries", copy.getDish());
        assertEquals(TicketStatus.COOKED, copy.getStatus());
        assertTrue(copy.toString().contains("FRYER"));
    }
}
