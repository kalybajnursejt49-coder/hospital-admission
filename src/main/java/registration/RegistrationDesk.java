package registration;

import java.sql.Connection;
import java.sql.SQLException;

public class RegistrationDesk {
    private final AdmissionValidator validator;
    private final InsuranceVerifier insuranceVerifier;
    private final AdmissionRepository repository;

    public RegistrationDesk(AdmissionValidator validator, InsuranceVerifier insuranceVerifier, AdmissionRepository repository) {
        this.validator = validator;
        this.insuranceVerifier = insuranceVerifier;
        this.repository = repository;
    }

    public Admission register(String name, String insuranceNumber) {
        validator.validate(name, insuranceNumber);
        insuranceVerifier.verify(insuranceNumber);
        return repository.saveAdmission(name.trim(), insuranceNumber.trim());
    }

    public Admission register(Connection connection, String name, String insuranceNumber) throws SQLException {
        validator.validate(name, insuranceNumber);
        insuranceVerifier.verify(insuranceNumber);
        return repository.saveAdmission(connection, name.trim(), insuranceNumber.trim());
    }
}
