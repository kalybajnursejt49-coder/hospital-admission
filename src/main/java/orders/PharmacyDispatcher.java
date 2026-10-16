package orders;

import exception.PharmacyOutOfStockException;

public class PharmacyDispatcher {
    private final OrderRepository repository;
    private final StockChecker checker;

    public PharmacyDispatcher(OrderRepository repository, StockChecker checker) {
        this.repository = repository;
        this.checker = checker;
    }

    public HospitalOrder dispatch(int admissionId, String medicine) {
        if (!checker.hasStock(repository.getMedicineStock(medicine))) {
            throw new PharmacyOutOfStockException("Препарат недоступен: " + medicine);
        }
        return repository.sendPharmacyOrder(admissionId, medicine);
    }
}
