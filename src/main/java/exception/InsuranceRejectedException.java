package exception;

public class InsuranceRejectedException extends InvalidPatientDataException {
    public InsuranceRejectedException(String message) {
        super(message);
    }
}
