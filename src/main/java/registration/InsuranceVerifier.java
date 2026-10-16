package registration;

import exception.InsuranceRejectedException;

public class InsuranceVerifier {
    public void verify(String insuranceNumber) {
        if (!insuranceNumber.startsWith("INS-") || insuranceNumber.length() <= 4) {
            throw new InsuranceRejectedException("Страховка не подтверждена");
        }
    }
}
