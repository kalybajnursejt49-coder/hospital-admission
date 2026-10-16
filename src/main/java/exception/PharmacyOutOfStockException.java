package exception;

public class PharmacyOutOfStockException extends RuntimeException {
    public PharmacyOutOfStockException(String message) {
        super(message);
    }
}
