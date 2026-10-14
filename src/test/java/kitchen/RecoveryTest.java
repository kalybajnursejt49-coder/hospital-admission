package kitchen;

import kitchen.cooking.CookingLine;
import kitchen.cooking.CookingRepository;
import kitchen.cooking.QualityChecker;
import kitchen.cooking.StationAllocator;
import kitchen.cooking.TicketStatus;
import kitchen.delivery.BillStatus;
import kitchen.delivery.BillingModule;
import kitchen.delivery.DeliveryRepository;
import kitchen.delivery.PriceCalculator;
import kitchen.facade.ChainRepository;
import kitchen.facade.ChainStatus;
import kitchen.intake.IntakeRepository;
import kitchen.intake.InventoryChecker;
import kitchen.intake.OrderQueue;
import kitchen.intake.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecoveryTest {
    @TempDir
    Path dir;
    KitchenTestKit kitchen;

    @BeforeEach
    void setUp() {
        kitchen = new KitchenTestKit(dir);
    }

    @Test
    void restartAfterAOnlyCancelsOrder() {
        OrderQueue queue = new OrderQueue(new IntakeRepository(dir), new InventoryChecker());
        int id = queue.accept("burger", 2);
        new ChainRepository(dir).setStatus(id, ChainStatus.PENDING_B);

        kitchen.restartKitchen();
        assertEquals(1, kitchen.app().admin().countPending());
        assertEquals(1, kitchen.app().admin().recoverPending());

        kitchen.assertChainIs(id, ChainStatus.COMPENSATED);
        kitchen.assertOrderIs(id, OrderStatus.CANCELLED);
        kitchen.assertNoTicketFor(id);
        kitchen.assertStock("bun", 10);
    }

    @Test
    void restartAfterBCancelsTicketAndOrder() {
        int id = kitchen.app().admin().simulateCrashAfterB("pizza", 2, 90);

        kitchen.restartKitchen();
        kitchen.assertChainIs(id, ChainStatus.PENDING_C);
        assertEquals(1, kitchen.app().admin().recoverPending());

        kitchen.assertChainIs(id, ChainStatus.COMPENSATED);
        kitchen.assertTicketIs(id, TicketStatus.DISCARDED);
        kitchen.assertOrderIs(id, OrderStatus.CANCELLED);
        kitchen.assertStock("dough", 10);
        kitchen.assertStock("cheese", 20);
    }

    @Test
    void restartInMiddleOfCVoidsTheBill() {
        int id = kitchen.app().admin().simulateCrashAfterB("burger", 1, 90);
        BillingModule billing = new BillingModule(new DeliveryRepository(dir), new PriceCalculator());
        billing.issue(id, "burger", 1);
        billing.pay(id);

        kitchen.restartKitchen();
        kitchen.app().admin().recoverPending();

        kitchen.assertBillIs(id, BillStatus.VOIDED);
        kitchen.assertTicketIs(id, TicketStatus.DISCARDED);
        kitchen.assertOrderIs(id, OrderStatus.CANCELLED);
    }

    @Test
    void crashBetweenBAndChainUpdateIsStillUnwoundFromDisk() {
        OrderQueue queue = new OrderQueue(new IntakeRepository(dir), new InventoryChecker());
        int id = queue.accept("burger", 1);
        new ChainRepository(dir).setStatus(id, ChainStatus.PENDING_B);
        CookingLine line = new CookingLine(new CookingRepository(dir), new StationAllocator(), new QualityChecker());
        line.start(id, "burger", 90);

        kitchen.restartKitchen();
        kitchen.app().admin().recoverPending();

        kitchen.assertTicketIs(id, TicketStatus.DISCARDED);
        kitchen.assertOrderIs(id, OrderStatus.CANCELLED);
        kitchen.assertStock("bun", 10);
    }

    @Test
    void recoverIgnoresFinishedChains() {
        kitchen.customerOrders("burger", 1);
        kitchen.chefBurnsTheBurger();
        kitchen.restartKitchen();
        assertEquals(0, kitchen.app().admin().countPending());
        assertEquals(0, kitchen.app().admin().recoverPending());
    }

    @Test
    void crashedTicketBlocksStationUntilRecovery() {
        kitchen.app().admin().simulateCrashAfterB("burger", 1, 90);
        kitchen.restartKitchen();
        kitchen.app().admin().recoverPending();
        int id = kitchen.customerOrders("burger", 1);
        kitchen.assertChainIs(id, ChainStatus.COMPLETED);
    }
}
