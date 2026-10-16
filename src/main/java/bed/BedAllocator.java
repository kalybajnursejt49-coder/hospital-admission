package bed;

public class BedAllocator {
    private final BedRepository repository;
    private final EquipmentPolicy policy;

    public BedAllocator(BedRepository repository, EquipmentPolicy policy) {
        this.repository = repository;
        this.policy = policy;
    }

    public BedAssignment reserve(int admissionId, String department) {
        return repository.reserve(admissionId, department, policy.equipmentFor(department));
    }
}
