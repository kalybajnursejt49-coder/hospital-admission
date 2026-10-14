package kitchen;

import kitchen.cooking.CookingLine;
import kitchen.cooking.CookingRepository;
import kitchen.cooking.QualityChecker;
import kitchen.cooking.StationAllocator;
import kitchen.delivery.BillingModule;
import kitchen.delivery.DeliveryDispatcher;
import kitchen.delivery.DeliveryRepository;
import kitchen.delivery.PriceCalculator;
import kitchen.facade.AdminFacade;
import kitchen.facade.ChainRepository;
import kitchen.facade.OrderFacade;
import kitchen.intake.IntakeRepository;
import kitchen.intake.InventoryChecker;
import kitchen.intake.OrderQueue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class KitchenApp {
    private static final String[] FILES = {"orders.csv", "stock.csv", "tickets.csv", "bills.csv", "chains.csv"};

    private final OrderFacade orderFacade;
    private final AdminFacade adminFacade;

    public KitchenApp(Path dir) {
        OrderQueue queue = new OrderQueue(new IntakeRepository(dir), new InventoryChecker());
        CookingLine cooking = new CookingLine(new CookingRepository(dir), new StationAllocator(), new QualityChecker());
        DeliveryRepository deliveryRepository = new DeliveryRepository(dir);
        BillingModule billing = new BillingModule(deliveryRepository, new PriceCalculator());
        DeliveryDispatcher dispatcher = new DeliveryDispatcher(deliveryRepository);
        ChainRepository chains = new ChainRepository(dir);
        orderFacade = new OrderFacade(queue, cooking, billing, dispatcher, chains);
        adminFacade = new AdminFacade(queue, cooking, billing, chains);
    }

    public OrderFacade orders() {
        return orderFacade;
    }

    public AdminFacade admin() {
        return adminFacade;
    }

    public static void reset(Path dir) {
        for (String name : FILES) {
            try {
                Files.deleteIfExists(dir.resolve(name));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
