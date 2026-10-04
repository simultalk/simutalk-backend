package pe.upc.simutalk.enums;

/**
 * UNVERIFIED: not checked yet, or the issuer could not be reached.
 * VERIFIED: the issuer confirmed the credential. REJECTED: the issuer did not match it.
 */
public enum VerificationStatus {
    VERIFIED,
    UNVERIFIED,
    REJECTED
}
