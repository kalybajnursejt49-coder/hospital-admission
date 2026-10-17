package facade;

public class ChainRecord {
    private int admissionId;
    private ChainStatus status;

    public ChainRecord(int admissionId, ChainStatus status) {
        this.admissionId = admissionId;
        this.status = status;
    }

    public int getAdmissionId() { return admissionId; }
    public ChainStatus getStatus() { return status; }
}
