package kitchen.delivery;

import java.util.List;

public class BillingModule {
    private final DeliveryRepository repository;
    private final PriceCalculator calculator;

    public BillingModule(DeliveryRepository repository, PriceCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    public Bill issue(int orderId, String dish, int quantity) {
        List<Bill> bills = repository.load();
        for (Bill bill : bills) {
            if (bill.getOrderId() == orderId) {
                throw new DeliveryException("Order " + orderId + " already has a bill");
            }
        }
        Bill bill = new Bill(orderId, calculator.total(dish, quantity), "-", "-", BillStatus.ISSUED);
        bills.add(bill);
        repository.save(bills);
        return bill;
    }

    public void pay(int orderId) {
        change(orderId, BillStatus.PAID);
    }

    public void voidBill(int orderId) {
        change(orderId, BillStatus.VOIDED);
    }

    public Bill find(int orderId) {
        return repository.find(orderId);
    }

    public List<Bill> all() {
        return repository.load();
    }

    private void change(int orderId, BillStatus next) {
        List<Bill> bills = repository.load();
        for (Bill bill : bills) {
            if (bill.getOrderId() == orderId) {
                bill.changeStatus(next);
                repository.save(bills);
                return;
            }
        }
        throw new DeliveryException("No bill for order " + orderId);
    }
}
