package pe.upc.simutalk.services;

import java.math.BigDecimal;

/**
 * Score of a CERTIFICATION criterion from the number of the candidate's certifications that are
 * VERIFIED and not expired (profiles only reports the count; the value of it is decided here):
 * 0 → 0.0, 1 → 7.0, 2 → 8.5, 3 or more → 10.0.
 */
public final class CertificationScoringPolicy {

    private CertificationScoringPolicy() {
    }

    public static BigDecimal scoreFor(long verifiedCertifications) {
        if (verifiedCertifications <= 0) {
            return new BigDecimal("0.0");
        }
        if (verifiedCertifications == 1) {
            return new BigDecimal("7.0");
        }
        if (verifiedCertifications == 2) {
            return new BigDecimal("8.5");
        }
        return new BigDecimal("10.0");
    }
}
