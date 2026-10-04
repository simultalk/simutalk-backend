package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.VerificationStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A certification declared by a candidate. Belongs to the CandidateProfile aggregate.
 * <p>
 * Central rule: a certification without a credential code can never be VERIFIED.
 * It is always created UNVERIFIED.
 */
@Getter
@Entity
@Table(name = "certifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Certification extends AuditableModel {

    public static final int TITLE_MAX_LENGTH = 150;
    public static final int ISSUER_MAX_LENGTH = 100;
    public static final int CREDENTIAL_CODE_MAX_LENGTH = 100;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false, length = ISSUER_MAX_LENGTH)
    private String issuer;

    @Column(name = "credential_code", length = CREDENTIAL_CODE_MAX_LENGTH)
    private String credentialCode;

    @Column(name = "issued_at", nullable = false)
    private LocalDate issuedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    public Certification(String title, String issuer, String credentialCode, LocalDate issuedAt, LocalDate expiresAt) {
        this.title = requireText(title, "title", TITLE_MAX_LENGTH);
        this.issuer = requireText(issuer, "issuer", ISSUER_MAX_LENGTH);
        this.credentialCode = normalizeCode(credentialCode);
        if (issuedAt == null) {
            throw new IllegalArgumentException("Certification issue date is required");
        }
        if (issuedAt.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Certification issue date cannot be in the future");
        }
        if (expiresAt != null && expiresAt.isBefore(issuedAt)) {
            throw new IllegalArgumentException("Certification expiration date cannot be before its issue date");
        }
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.verificationStatus = VerificationStatus.UNVERIFIED;
    }

    public boolean hasCredentialCode() {
        return credentialCode != null && !credentialCode.isBlank();
    }

    public boolean isPendingVerification() {
        return verificationStatus == VerificationStatus.UNVERIFIED;
    }

    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }

    /**
     * @throws IllegalStateException if the certification has no credential code or was
     *                               already verified or rejected
     */
    public void markVerified(Instant verifiedAt) {
        if (!hasCredentialCode()) {
            throw new IllegalStateException("A certification without credential code can never be verified");
        }
        if (!isPendingVerification()) {
            throw new IllegalStateException("Only an UNVERIFIED certification can be verified (current: %s)"
                    .formatted(verificationStatus));
        }
        if (verifiedAt == null) {
            throw new IllegalArgumentException("Verification instant is required");
        }
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.verifiedAt = verifiedAt;
    }

    /**
     * @throws IllegalStateException if the certification is not UNVERIFIED
     */
    public void markRejected() {
        if (!isPendingVerification()) {
            throw new IllegalStateException("Only an UNVERIFIED certification can be rejected (current: %s)"
                    .formatted(verificationStatus));
        }
        this.verificationStatus = VerificationStatus.REJECTED;
        this.verifiedAt = null;
    }

    public boolean isExpired(LocalDate today) {
        return expiresAt != null && expiresAt.isBefore(today);
    }

    public boolean countsForScoring(LocalDate today) {
        return isVerified() && !isExpired(today);
    }

    /** VERIFIED and not expired as of today. */
    public boolean countsForScoring() {
        return countsForScoring(LocalDate.now());
    }

    public boolean hasSameCredentialAs(String otherIssuer, String otherCode) {
        var normalized = normalizeCode(otherCode);
        return hasCredentialCode() && normalized != null
                && credentialCode.equalsIgnoreCase(normalized)
                && otherIssuer != null && issuer.equalsIgnoreCase(otherIssuer.strip());
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        var stripped = code.strip();
        if (stripped.length() > CREDENTIAL_CODE_MAX_LENGTH) {
            throw new IllegalArgumentException("Credential code must be at most %d characters"
                    .formatted(CREDENTIAL_CODE_MAX_LENGTH));
        }
        return stripped;
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Certification %s is required".formatted(field));
        }
        var stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new IllegalArgumentException("Certification %s must be at most %d characters".formatted(field, maxLength));
        }
        return stripped;
    }
}
