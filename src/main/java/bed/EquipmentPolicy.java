package bed;

public class EquipmentPolicy {
    public String equipmentFor(String department) {
        if ("ICU".equalsIgnoreCase(department)) {
            return "MONITOR";
        }
        return "BASIC";
    }
}
