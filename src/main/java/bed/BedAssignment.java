package bed;

public class BedAssignment {
    public enum Status { RESERVED, ASSIGNED, RELEASED }

    private int admissionId;
    private String department;
    private String equipment;
    private Status status;

    public BedAssignment(int admissionId, String department, String equipment, Status status) {
        this.admissionId = admissionId;
        this.department = department;
        this.equipment = equipment;
        this.status = status;
    }

    public int getAdmissionId() { return admissionId; }
    public void setAdmissionId(int admissionId) { this.admissionId = admissionId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
