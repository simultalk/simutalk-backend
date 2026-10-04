package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.CriterionKind;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Score of one criterion. Belongs to the Assessment aggregate.
 * <p>
 * A COMPETENCY score always carries at least one evidence ("a score without evidence is not
 * shown"); a CERTIFICATION score comes from verified certifications and has none.
 * Scores are 0.0 to 10.0 (one decimal) and confidence 0.00 to 1.00.
 */
@Getter
@Entity
@Table(name = "criterion_scores")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CriterionScore extends AuditableModel {

    public static final BigDecimal MAX_SCORE = BigDecimal.TEN;

    @Column(name = "criterion_id", nullable = false)
    private Long criterionId;

    /** Snapshot of the criterion name; criteria are frozen once the job posting is published. */
    @Column(name = "criterion_name", nullable = false, length = 100)
    private String criterionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterion_kind", nullable = false, length = 20)
    private CriterionKind criterionKind;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal score;

    @Column(name = "weight_applied", nullable = false)
    private int weightApplied;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal confidence;

    @Getter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "criterion_score_id", nullable = false)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    private List<Evidence> evidences = new ArrayList<>();

    public CriterionScore(Long criterionId, String criterionName, CriterionKind criterionKind, BigDecimal score,
                          int weightApplied, BigDecimal confidence, List<Evidence> evidences) {
        if (criterionId == null || criterionId <= 0) {
            throw new IllegalArgumentException("Criterion id must be a positive number");
        }
        if (criterionName == null || criterionName.isBlank()) {
            throw new IllegalArgumentException("Criterion name is required");
        }
        if (criterionKind == null) {
            throw new IllegalArgumentException("Criterion kind is required");
        }
        if (weightApplied < 1 || weightApplied > 100) {
            throw new IllegalArgumentException("Applied weight must be between 1 and 100");
        }
        var safeEvidences = evidences == null ? List.<Evidence>of() : evidences;
        if (criterionKind == CriterionKind.COMPETENCY && safeEvidences.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "COMPETENCY criterion '%s' needs at least one evidence to be scored".formatted(criterionName));
        }
        this.criterionId = criterionId;
        this.criterionName = criterionName.strip();
        this.criterionKind = criterionKind;
        this.score = requireRange(score, BigDecimal.ZERO, MAX_SCORE, "Score").setScale(1, RoundingMode.HALF_UP);
        this.weightApplied = weightApplied;
        this.confidence = requireRange(confidence, BigDecimal.ZERO, BigDecimal.ONE, "Confidence").setScale(2, RoundingMode.HALF_UP);
        this.evidences.addAll(safeEvidences);
    }

    public List<Evidence> getEvidences() {
        return Collections.unmodifiableList(evidences);
    }

    /** score × weight, before dividing by 100. */
    public BigDecimal weightedContribution() {
        return score.multiply(BigDecimal.valueOf(weightApplied));
    }

    private static BigDecimal requireRange(BigDecimal value, BigDecimal min, BigDecimal max, String field) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException("%s must be between %s and %s".formatted(field, min, max));
        }
        return value;
    }
}
