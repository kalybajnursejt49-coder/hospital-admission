package orders;

public class LabOrderSystem {
    private final OrderRepository repository;

    public LabOrderSystem(OrderRepository repository) {
        this.repository = repository;
    }

    public HospitalOrder createAndSend(int admissionId) {
        HospitalOrder order = repository.saveOrder(admissionId, "LAB", "");
        repository.markSent(order.getId());
        order.send();
        return order;
    }
}
