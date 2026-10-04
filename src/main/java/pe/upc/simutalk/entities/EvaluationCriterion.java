package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.CriterionOrigin;
import pe.upc.simutalk.enums.CriterionType;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A criterion the company uses to evaluate candidates for a job posting.
 * Belongs to the {@link JobPosting} aggregate: it is created, changed and removed
 * only through the aggregate root.
 */
@Getter
@Entity
@Table(name = "evaluation_criteria")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationCriterion extends AuditableModel {

    public static final int NAME_MAX_LENGTH = 100;
    public static final int DESCRIPTION_MAX_LENGTH = 1000;
    public static final int CERTIFICATION_NAME_MAX_LENGTH = 150;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false, length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Embedded
    private Weight weight;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterion_type", nullable = false, length = 20)
    private CriterionType criterionType;

    /** Only set when {@link #criterionType} is CERTIFICATION. */
    @Column(name = "certification_name", length = CERTIFICATION_NAME_MAX_LENGTH)
    private String certificationName;

    /** Only meaningful when {@link #criterionType} is CERTIFICATION. */
    @Column(nullable = false)
    private boolean mandatory;

    /** Who proposed the criterion; rows created before this column existed read as MANUAL. */
    @Getter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CriterionOrigin origin;

    public EvaluationCriterion(JobPosting jobPosting, String name, String description, Weight weight,
                               CriterionType criterionType, String certificationName, boolean mandatory) {
        this(jobPosting, name, description, weight, criterionType, certificationName, mandatory, CriterionOrigin.MANUAL);
    }

    /**
     * @param weight always set by the recruiter, also for an {@code AI_SUGGESTED} criterion: it is required
     * @param origin {@code null} means MANUAL
     */
    public EvaluationCriterion(JobPosting jobPosting, String name, String description, Weight weight,
                               CriterionType criterionType, String certificationName, boolean mandatory,
                               CriterionOrigin origin) {
        if (jobPosting == null) {
            throw new IllegalArgumentException("Evaluation criterion must belong to a job posting");
        }
        this.jobPosting = jobPosting;
        this.origin = origin == null ? CriterionOrigin.MANUAL : origin;
        applyDetails(name, description, weight, criterionType, certificationName, mandatory);
    }

    /**
     * Changes the criterion. Intended to be called only by {@link JobPosting}, which
     * guards the aggregate-level rules (status, unique names).
     */
    public void update(String name, String description, Weight weight, CriterionType criterionType,
                       String certificationName, boolean mandatory) {
        applyDetails(name, description, weight, criterionType, certificationName, mandatory);
    }

    public CriterionOrigin getOrigin() {
        return origin == null ? CriterionOrigin.MANUAL : origin;
    }

    public Long getJobPostingId() {
        return jobPosting.getId();
    }

    public boolean isCertification() {
        return criterionType == CriterionType.CERTIFICATION;
    }

    public boolean hasName(String otherName) {
        return otherName != null && name.equalsIgnoreCase(otherName.strip());
    }

    private void applyDetails(String name, String description, Weight weight, CriterionType criterionType,
                              String certificationName, boolean mandatory) {
        if (weight == null) {
            throw new IllegalArgumentException("Evaluation criterion weight is required");
        }
        if (criterionType == null) {
            throw new IllegalArgumentException("Evaluation criterion type is required");
        }
        this.name = requireText(name, "name", NAME_MAX_LENGTH);
        this.description = requireText(description, "description", DESCRIPTION_MAX_LENGTH);
        this.weight = weight;
        this.criterionType = criterionType;
        if (criterionType == CriterionType.CERTIFICATION) {
            this.certificationName = requireText(certificationName, "certificationName", CERTIFICATION_NAME_MAX_LENGTH);
            this.mandatory = mandatory;
        } else {
            // certification data does not apply to competencies
            this.certificationName = null;
            this.mandatory = false;
        }
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Evaluation criterion %s is required".formatted(field));
        }
        var stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new IllegalArgumentException("Evaluation criterion %s must be at most %d characters".formatted(field, maxLength));
        }
        return stripped;
    }
}
