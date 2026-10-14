# Restaurant Kitchen: Facade over a three-stage chain

Assignment 4, variant 14 (Restaurant Kitchen).

| Stage | Subsystem | Package | Parts from the variant table |
|---|---|---|---|
| A: intake / reservation | Order intake | `kitchen.intake` | Order Queue, Inventory Checker |
| B: processing / execution | Cooking | `kitchen.cooking` | Cooking Station Allocator, Quality Checker |
| C: finalization / confirmation | Billing and delivery | `kitchen.delivery` | Billing Module, Delivery Dispatcher |

Facades are in `kitchen.facade`: `OrderFacade` (client workflow A -> B -> C) and `AdminFacade` (inspection, manual compensation, recovery).

## Run

Requires Java 17+ and Maven.

```
mvn compile exec:java
```

Tests and coverage report (`target/site/jacoco/index.html`):

```
mvn verify
```

Reset the persisted store before a fresh run: use menu option 11, or delete the `data/` folder.

## Demo menu

```
1  Place order                       7  Recover unfinished chains
2  Show order report                 8  Demo: B fails after A (bad quality)
3  List chains                       9  Demo: C fails after A and B (zone not served)
4  Show stock                        10 Demo: crash after B (then restart, then option 7)
5  Restock ingredient                11 Reset data
6  Compensate order manually         0  Exit
```

Options 8 and 9 trigger the two mandatory compensation depths on demand. Option 1 does the same with real input: chef quality score below 60 makes B fail, a zone other than CENTER / NORTH / SOUTH makes C fail, and ordering more than the stock allows makes A fail (nothing committed yet).

## Chain and persistence

Every subsystem has its own CSV file in `data/` (`orders.csv`, `stock.csv`, `tickets.csv`, `bills.csv`). The facade keeps the chain progress in `chains.csv`:

```
placeOrder
  A: OrderQueue.accept            chain = PENDING_B
  B: CookingLine.start            chain = PENDING_C
  C: BillingModule.issue -> DeliveryDispatcher.dispatch -> BillingModule.pay
  A: OrderQueue.complete          chain = COMPLETED
```

Failures:

| Failure | Where | What the facade does |
|---|---|---|
| Unknown dish, bad quantity, not enough stock | A | Nothing was committed, throws `OrderFailedException("A")` |
| No free station or quality score under 60 | B, after A committed | Compensates A alone: order CANCELLED, stock restored |
| Zone not served | C, after A and B committed | Compensates in order: bill VOIDED, ticket DISCARDED, order CANCELLED |
| Empty dish or zone, bad numbers | Before A | `InvalidRequestException` |

`Compensator` does not trust memory. It reads the bill, the ticket and the order from disk and undoes only what is actually there and not yet undone, always in the order C, B, A. Running it twice does nothing the second time. That is why the same code handles a failure inside the program and a crash found after a restart.

After a restart the program prints how many chains are still `PENDING_B` or `PENDING_C`. Option 7 (`AdminFacade.recoverPending`) unwinds them from the persisted state.

## Subsystem rules

- No subsystem imports another one. Only `kitchen.facade` knows all three. They share only `FileStore`, a tiny file helper.
- Each subsystem has: an entity with a status and its own legal transitions (`Order`, `Ticket`, `Bill`), at least one pure business-logic class (`InventoryChecker`, `StationAllocator`, `QualityChecker`, `PriceCalculator`), and its own persistence gateway (`IntakeRepository`, `CookingRepository`, `DeliveryRepository`).
- Each subsystem is tested alone without the facade: `IntakeTest`, `CookingTest`, `DeliveryTest`.

## Tests

63 JUnit 5 tests. Every test gets a fresh `@TempDir`, so tests are independent and repeatable and run in about a second. Each test asserts its result.

- `KitchenTestKit` is the domain test language: `kitchen.chefBurnsTheBurger()`, `kitchen.courierCannotReachCustomer()`, `kitchen.assertBillIs(id, BillStatus.VOIDED)`, `kitchen.restartKitchen()`.
- `OrderFacadeTest`: `failureInBCompensatesOnlyA` and `failureInCCompensatesBThenA` check the persisted end state of both depths.
- `RecoveryTest`: the persistence-reload tests. They stop mid-chain, build a new `KitchenApp` on the same folder (a restart) and check that recovery uses the status on disk.
- Coverage: JaCoCo, line coverage 98.6% over the three subsystems and the facades (560 of 568 lines). The CLI class `Main` is excluded and `mvn verify` fails below 80%.

## Principle of Least Knowledge: before and after

Before, without a facade, the client talks to all three subsystems and to the details inside them:

```java
OrderQueue queue = new OrderQueue(new IntakeRepository(dir), new InventoryChecker());
CookingLine cooking = new CookingLine(new CookingRepository(dir), new StationAllocator(), new QualityChecker());
DeliveryRepository repo = new DeliveryRepository(dir);
BillingModule billing = new BillingModule(repo, new PriceCalculator());
DeliveryDispatcher dispatcher = new DeliveryDispatcher(repo);

int id = queue.accept("burger", 1);
try {
    cooking.start(id, "burger", 90);
    billing.issue(id, "burger", 1);
    dispatcher.dispatch(id, "CENTER");
    billing.pay(id);
    queue.complete(id);
} catch (CookingException e) {
    queue.cancel(id);
} catch (DeliveryException e) {
    billing.voidBill(id);
    cooking.discard(id);
    queue.cancel(id);
}
```

The client has about ten friends, knows the order of the steps, and has to write the rollback itself. It also forgets to persist the chain progress.

After, the client has one friend:

```java
int id = app.orders().placeOrder("burger", 1, 90, "CENTER");
```

`Main` knows only `KitchenApp`, `OrderFacade` and `AdminFacade`. It never touches `OrderQueue`, `CookingLine` or `BillingModule`.

## Facade vs Adapter

An Adapter exists to make one existing interface look like another interface the client already expects: the client wants `Target`, we have an `Adaptee` with different method names, and the adapter translates. The intent is compatibility, and it is usually one-to-one.

Here nobody expects a different interface. `OrderQueue`, `CookingLine` and `BillingModule` already have clean interfaces. The problem is that the client would need all three, in the right order, and would have to undo the right part when a later step fails. The intent is to give one simple entry point and to hide a workflow with sequencing, persisted progress and compensation. That is the intent of Facade: simplify a group of subsystems, not translate one interface. If we later wrapped a third-party payment API inside `BillingModule`, that wrapper would be an Adapter, and the facade would still be needed on top of it.

## UML

`docs/uml-class-diagram.png` (also `.svg`; source in `.dot`). There are no arrows between the three subsystem boxes, only through the facades.
