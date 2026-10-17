package registration;

public class Admission {
    public enum Status { NEW, REGISTERED, CANCELLED }

    private int id;
    private String patientName;
    private String insuranceNumber;
    private Status status;

    public Admission(int id, String patientName, String insuranceNumber, Status status) {
        this.id = id;
        this.patientName = patientName;
        this.insuranceNumber = insuranceNumber;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getPatientName() { return patientName; }
    public String getInsuranceNumber() { return insuranceNumber; }
    public Status getStatus() { return status; }
    public void register() {
        if (status != Status.NEW) {
            throw new IllegalStateException("Регистрация возможна только из NEW");
        }
        status = Status.REGISTERED;
    }

    public void cancel() {
        if (status != Status.REGISTERED) {
            throw new IllegalStateException("Отмена возможна только из REGISTERED");
        }
        status = Status.CANCELLED;
    }
}
