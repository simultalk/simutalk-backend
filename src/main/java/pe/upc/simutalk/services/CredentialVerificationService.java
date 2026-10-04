package pe.upc.simutalk.services;

/**
 * Port to check a credential against its issuer (Coursera, Credly, CertiProf...).
 * Implementations never throw: when the issuer cannot be reached they answer with
 * {@link VerificationResult#unavailable(String)} so the certification stays UNVERIFIED.
 */
public interface CredentialVerificationService {

    VerificationResult verify(String issuer, String credentialCode, String title, String holderName);

    /**
     * @param matched    whether the issuer confirmed the credential
     * @param detail     human readable explanation
     * @param conclusive {@code false} when the issuer could not be consulted (timeout, errors,
     *                   issuer not supported); the certification must then stay UNVERIFIED
     */
    record VerificationResult(boolean matched, String detail, boolean conclusive) {

        public VerificationResult(boolean matched, String detail) {
            this(matched, detail, true);
        }

        public static VerificationResult unavailable(String detail) {
            return new VerificationResult(false, detail, false);
        }
    }
}
