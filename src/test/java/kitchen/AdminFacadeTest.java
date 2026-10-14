package kitchen;

import kitchen.cooking.TicketStatus;
import kitchen.delivery.BillStatus;
import kitchen.facade.ChainStatus;
import kitchen.facade.InvalidRequestException;
import kitchen.intake.IntakeException;
import kitchen.intake.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminFacadeTest {
    @TempDir
    Path dir;
    KitchenTestKit kitchen;

    @BeforeEach
    void setUp() {
        kitchen = new KitchenTestKit(dir);
    }

    @Test
    void reportShowsAllThreeSubsystems() {
        int id = kitchen.customerOrders("salad", 1);
        List<String> report = kitchen.app().admin().report(id);
        assertEquals(4, report.size());
        assertTrue(report.get(0).contains("COMPLETED"));
        assertTrue(report.get(1).startsWith("A:"));
        assertTrue(report.get(2).startsWith("B:"));
        assertTrue(report.get(3).startsWith("C:"));
    }

    @Test
    void reportForUnknownOrder() {
        List<String> report = kitchen.app().admin().report(42);
        assertEquals(1, report.size());
        assertTrue(report.get(0).contains("No chain"));
    }

    @Test
    void reportAfterFailureShowsNothingForSkippedStages() {
        int id = kitchen.chefBurnsTheBurger();
        List<String> report = kitchen.app().admin().report(id);
        assertTrue(report.get(3).contains("nothing"));
    }

    @Test
    void crashedChainIsCountedAsPending() {
        kitchen.app().admin().simulateCrashAfterB("pizza", 1, 90);
        assertEquals(1, kitchen.app().admin().countPending());
    }

    @Test
    void listChainsShowsEveryOrder() {
        kitchen.customerOrders("burger", 1);
        kitchen.chefBurnsTheBurger();
        List<String> chains = kitchen.app().admin().listChains();
        assertEquals(2, chains.size());
        assertEquals("Order #2 -> COMPENSATED", chains.get(1));
    }

    @Test
    void manualCompensationOfCrashedChainUnwindsEverything() {
        int id = kitchen.app().admin().simulateCrashAfterB("pizza", 1, 90);
        List<String> steps = kitchen.app().admin().compensate(id);
        assertEquals(2, steps.size());
        assertTrue(steps.get(0).startsWith("B"));
        assertTrue(steps.get(1).startsWith("A"));
        kitchen.assertChainIs(id, ChainStatus.COMPENSATED);
        kitchen.assertTicketIs(id, TicketStatus.DISCARDED);
        kitchen.assertOrderIs(id, OrderStatus.CANCELLED);
        kitchen.assertStock("dough", 10);
    }

    @Test
    void compensatingTwiceDoesNothingSecondTime() {
        int id = kitchen.app().admin().simulateCrashAfterB("pizza", 1, 90);
        kitchen.app().admin().compensate(id);
        assertTrue(kitchen.app().admin().compensate(id).isEmpty());
        kitchen.assertStock("dough", 10);
    }

    @Test
    void completedOrderCannotBeCompensated() {
        int id = kitchen.customerOrders("burger", 1);
        assertThrows(InvalidRequestException.class, () -> kitchen.app().admin().compensate(id));
        kitchen.assertBillIs(id, BillStatus.PAID);
    }

    @Test
    void unknownOrderCannotBeCompensated() {
        assertThrows(InvalidRequestException.class, () -> kitchen.app().admin().compensate(77));
    }

    @Test
    void restockChangesStock() {
        kitchen.app().admin().restock(" Bun ", 5);
        kitchen.assertStock("bun", 15);
        assertThrows(IntakeException.class, () -> kitchen.app().admin().restock("bun", -3));
    }

    @Test
    void stockListsAllIngredients() {
        assertEquals(7, kitchen.app().admin().stock().size());
    }

    @Test
    void resetDeletesPersistedState() {
        kitchen.customerOrders("burger", 1);
        KitchenApp.reset(dir);
        kitchen.restartKitchen();
        assertTrue(kitchen.app().admin().listChains().isEmpty());
        kitchen.assertStock("bun", 10);
    }
}
