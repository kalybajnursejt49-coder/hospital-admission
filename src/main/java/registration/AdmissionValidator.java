package registration;

import exception.InvalidPatientDataException;

public class AdmissionValidator {
    public void validate(String name, String insuranceNumber) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidPatientDataException("Имя пациента не указано");
        }
        if (insuranceNumber == null || insuranceNumber.trim().isEmpty()) {
            throw new InvalidPatientDataException("Страховой номер не указан");
        }
    }
}
