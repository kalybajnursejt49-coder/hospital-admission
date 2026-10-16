package bed;

public class EquipmentReserver {
    private final BedRepository repository;

    public EquipmentReserver(BedRepository repository) {
        this.repository = repository;
    }

    public void reserve(int admissionId) {
        repository.assignEquipment(admissionId);
    }
}
