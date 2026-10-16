package orders;

public class HospitalOrder {
    public enum Status { CREATED, SENT, CANCELLED }

    private int id;
    private int admissionId;
    private String type;
    private String medicine;
    private Status status;

    public HospitalOrder(int id, int admissionId, String type, String medicine, Status status) {
        this.id = id;
        this.admissionId = admissionId;
        this.type = type;
        this.medicine = medicine;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getAdmissionId() { return admissionId; }
    public void setAdmissionId(int admissionId) { this.admissionId = admissionId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMedicine() { return medicine; }
    public void setMedicine(String medicine) { this.medicine = medicine; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
