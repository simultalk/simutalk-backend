package pe.upc.simutalk.entities;

import pe.upc.simutalk.dtos.CreateCandidateProfileCommand;
import pe.upc.simutalk.enums.VerificationStatus;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Profile of a candidate (applicant), owned by a user of the iam context referenced
 * only by {@code userId}. Holds the candidate's certifications.
 * <p>
 * Invariants: the candidate is at least 18 years old; years of experience are not
 * negative; the document number cannot change once created; a certification can only
 * be verified while UNVERIFIED and only if it has a credential code; the same
 * credential (issuer + code) cannot be registered twice.
 * <p>
 * No scoring logic lives here: how much certifications are worth is decided in assessment.
 */
@Getter
@Entity
@Table(name = "candidates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CandidateProfile extends AuditableAggregateRoot<CandidateProfile> {

    public static final int MINIMUM_AGE = 18;
    public static final int PHONE_MAX_LENGTH = 20;
    public static final int DISTRICT_MAX_LENGTH = 80;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Embedded
    private PersonName personName;

    @Embedded
    private DocumentNumber documentNumber;

    /** Optional contact e-mail (personal data: never sent to the AI provider). */
    @Embedded
    private EmailAddress email;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = PHONE_MAX_LENGTH)
    private String phone;

    @Column(nullable = false, length = DISTRICT_MAX_LENGTH)
    private String district;

    @Column(name = "years_of_experience", nullable = false)
    private int yearsOfExperience;

    @Getter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "candidate_id", nullable = false)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    private List<Certification> certifications = new ArrayList<>();

    public CandidateProfile(CreateCandidateProfileCommand command) {
        if (command.userId() == null || command.userId() <= 0) {
            throw new IllegalArgumentException("User id must be a positive number");
        }
        this.userId = command.userId();
        this.documentNumber = new DocumentNumber(command.documentNumber());
        applyDetails(new PersonName(command.firstName(), command.lastName()), command.birthDate(), command.phone(),
                command.district(), command.yearsOfExperience());
        this.email = EmailAddress.ofNullable(command.email());
    }

    /** Changes the editable data; the e-mail is kept as it is. */
    public void updateDetails(PersonName personName, LocalDate birthDate, String phone, String district,
                              int yearsOfExperience) {
        applyDetails(personName, birthDate, phone, district, yearsOfExperience);
    }

    /** Changes the editable data, including the e-mail ({@code null} or blank removes it). */
    public void updateDetails(PersonName personName, LocalDate birthDate, String phone, String district,
                              int yearsOfExperience, String email) {
        applyDetails(personName, birthDate, phone, district, yearsOfExperience);
        this.email = EmailAddress.ofNullable(email);
    }

    public boolean isOwnedBy(Long otherUserId) {
        return userId.equals(otherUserId);
    }

    public int getAge(LocalDate today) {
        return Period.between(birthDate, today).getYears();
    }

    /* ---------- certifications ---------- */

    public List<Certification> getCertifications() {
        return Collections.unmodifiableList(certifications);
    }

    public Certification addCertification(Certification certification) {
        if (certification == null) {
            throw new IllegalArgumentException("Certification is required");
        }
        if (certification.hasCredentialCode() && certifications.stream()
                .anyMatch(existing -> existing.hasSameCredentialAs(certification.getIssuer(),
                        certification.getCredentialCode()))) {
            throw new BusinessRuleViolationException(
                    "This credential from %s is already registered".formatted(certification.getIssuer()));
        }
        certifications.add(certification);
        return certification;
    }

    public Certification getCertification(Long certificationId) {
        return certifications.stream()
                .filter(certification -> certification.getId() != null && certification.getId().equals(certificationId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Certification %s not found for candidate %s".formatted(certificationId, getId())));
    }

    /**
     * Checks, before contacting the issuer, that the certification can be verified.
     *
     * @throws BusinessRuleViolationException if it has no credential code or is not UNVERIFIED
     */
    public Certification ensureCertificationCanBeVerified(Long certificationId) {
        var certification = getCertification(certificationId);
        if (!certification.hasCredentialCode()) {
            throw new BusinessRuleViolationException(
                    "A certification without credential code can never be verified");
        }
        if (!certification.isPendingVerification()) {
            throw new BusinessRuleViolationException("Certification is already %s"
                    .formatted(certification.getVerificationStatus()));
        }
        return certification;
    }

    public Certification verifyCertification(Long certificationId, Instant verifiedAt) {
        var certification = ensureCertificationCanBeVerified(certificationId);
        certification.markVerified(verifiedAt);
        return certification;
    }

    public Certification rejectCertification(Long certificationId) {
        var certification = getCertification(certificationId);
        if (!certification.isPendingVerification()) {
            throw new BusinessRuleViolationException("Certification is already %s"
                    .formatted(certification.getVerificationStatus()));
        }
        certification.markRejected();
        return certification;
    }

    public void removeCertification(Long certificationId) {
        certifications.remove(getCertification(certificationId));
    }

    /** Certifications in VERIFIED status, expired or not. */
    public long countVerifiedCertifications() {
        return certifications.stream().filter(Certification::isVerified).count();
    }

    /** Declared certifications the issuer did not match. */
    public long countRejectedCertifications() {
        return certifications.stream()
                .filter(certification -> certification.getVerificationStatus() == VerificationStatus.REJECTED)
                .count();
    }

    /** Certifications that are VERIFIED and not expired as of {@code today}. */
    public long countCertificationsForScoring(LocalDate today) {
        return certifications.stream().filter(certification -> certification.countsForScoring(today)).count();
    }

    /* ---------- helpers ---------- */

    private void applyDetails(PersonName personName, LocalDate birthDate, String phone, String district,
                              int yearsOfExperience) {
        if (personName == null) {
            throw new IllegalArgumentException("Candidate name is required");
        }
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date is required");
        }
        if (Period.between(birthDate, LocalDate.now()).getYears() < MINIMUM_AGE) {
            throw new IllegalArgumentException("Candidate must be at least %d years old".formatted(MINIMUM_AGE));
        }
        if (yearsOfExperience < 0) {
            throw new IllegalArgumentException("Years of experience cannot be negative");
        }
        this.personName = personName;
        this.birthDate = birthDate;
        this.phone = requireText(phone, "phone", PHONE_MAX_LENGTH);
        this.district = requireText(district, "district", DISTRICT_MAX_LENGTH);
        this.yearsOfExperience = yearsOfExperience;
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Candidate %s is required".formatted(field));
        }
        var stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new IllegalArgumentException("Candidate %s must be at most %d characters".formatted(field, maxLength));
        }
        return stripped;
    }
}
