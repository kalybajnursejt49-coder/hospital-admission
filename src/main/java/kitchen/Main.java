package kitchen;

import kitchen.facade.InvalidRequestException;
import kitchen.facade.OrderFailedException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static final Path DATA_DIR = Paths.get("data");

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        KitchenApp app = new KitchenApp(DATA_DIR);
        int pending = app.admin().countPending();
        if (pending > 0) {
            System.out.println("Found " + pending + " unfinished chain(s) from the last run. Use option 7 to recover.");
        }
        boolean running = true;
        while (running) {
            printMenu();
            String choice = in.nextLine().trim();
            try {
                if (choice.equals("1")) {
                    placeOrder(app, in);
                } else if (choice.equals("2")) {
                    int id = askInt(in, "Order id: ");
                    printLines(app.admin().report(id));
                } else if (choice.equals("3")) {
                    printLines(app.admin().listChains());
                } else if (choice.equals("4")) {
                    printStock(app);
                } else if (choice.equals("5")) {
                    System.out.print("Ingredient: ");
                    String ingredient = in.nextLine();
                    int amount = askInt(in, "Amount: ");
                    app.admin().restock(ingredient, amount);
                    System.out.println("Stock updated");
                } else if (choice.equals("6")) {
                    int id = askInt(in, "Order id: ");
                    printLines(app.admin().compensate(id));
                } else if (choice.equals("7")) {
                    System.out.println("Recovered chains: " + app.admin().recoverPending());
                } else if (choice.equals("8")) {
                    runOrder(app, "burger", 1, 30, "CENTER");
                } else if (choice.equals("9")) {
                    runOrder(app, "burger", 1, 90, "MARS");
                } else if (choice.equals("10")) {
                    int id = app.admin().simulateCrashAfterB("pizza", 1, 90);
                    System.out.println("Crash simulated after B for order #" + id + ". Restart the program, then use option 7.");
                } else if (choice.equals("11")) {
                    KitchenApp.reset(DATA_DIR);
                    app = new KitchenApp(DATA_DIR);
                    System.out.println("Data reset");
                } else if (choice.equals("0")) {
                    running = false;
                } else {
                    System.out.println("Unknown option");
                }
            } catch (OrderFailedException e) {
                System.out.println(e.getMessage());
            } catch (InvalidRequestException e) {
                System.out.println("Invalid request: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1  Place order");
        System.out.println("2  Show order report");
        System.out.println("3  List chains");
        System.out.println("4  Show stock");
        System.out.println("5  Restock ingredient");
        System.out.println("6  Compensate order manually");
        System.out.println("7  Recover unfinished chains");
        System.out.println("8  Demo: B fails after A (bad quality)");
        System.out.println("9  Demo: C fails after A and B (zone not served)");
        System.out.println("10 Demo: crash after B");
        System.out.println("11 Reset data");
        System.out.println("0  Exit");
        System.out.print("> ");
    }

    private static void placeOrder(KitchenApp app, Scanner in) {
        System.out.print("Dish (burger, pizza, salad, fries): ");
        String dish = in.nextLine();
        int quantity = askInt(in, "Quantity: ");
        int quality = askInt(in, "Chef quality score 0-100 (60+ passes): ");
        System.out.print("Zone (CENTER, NORTH, SOUTH): ");
        String zone = in.nextLine();
        runOrder(app, dish, quantity, quality, zone);
    }

    private static void runOrder(KitchenApp app, String dish, int quantity, int quality, String zone) {
        try {
            int id = app.orders().placeOrder(dish, quantity, quality, zone);
            System.out.println("Order #" + id + " completed");
            printLines(app.admin().report(id));
        } catch (OrderFailedException e) {
            System.out.println(e.getMessage());
            if (e.getOrderId() > 0) {
                System.out.println("Compensation done. State on disk:");
                printLines(app.admin().report(e.getOrderId()));
            }
        }
    }

    private static int askInt(Scanner in, String text) {
        System.out.print(text);
        try {
            return Integer.parseInt(in.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("A number is required");
        }
    }

    private static void printLines(List<String> lines) {
        if (lines.isEmpty()) {
            System.out.println("Nothing to show");
        }
        for (String line : lines) {
            System.out.println(line);
        }
    }

    private static void printStock(KitchenApp app) {
        Map<String, Integer> stock = app.admin().stock();
        for (String ingredient : stock.keySet()) {
            System.out.println(ingredient + ": " + stock.get(ingredient));
        }
    }
}
