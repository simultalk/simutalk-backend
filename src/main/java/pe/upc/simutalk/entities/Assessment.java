package pe.upc.simutalk.entities;

import pe.upc.simutalk.dtos.InterviewSessionSnapshot;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Result of scoring one completed interview against the job posting's weighted criteria.
 * <p>
 * Invariants: only a COMPLETED interview session can be assessed (one assessment per
 * session); every criterion appears once and the applied weights add up to 100;
 * {@code weightedScore = Σ(score × weightApplied) / 100}, computed here and rounded to one
 * decimal (HALF_UP).
 * <p>
 * {@code applicationId}, {@code jobPostingId} and {@code candidateId} are immutable copies taken
 * from the session, used for rankings and authorization.
 * <p>
 * {@code feedbackSummary} is the text shown to the candidate: what sustained the score and where
 * points were lost. It never mentions the ranking, other candidates or integrity flags.
 */
@Getter
@Entity
@Table(name = "assessments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assessment extends AuditableAggregateRoot<Assessment> {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Column(name = "interview_session_id", nullable = false, unique = true)
    private Long interviewSessionId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "weighted_score", nullable = false, precision = 3, scale = 1)
    private BigDecimal weightedScore;

    @Column(name = "engine_version", nullable = false, length = 60)
    private String engineVersion;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    public static final int FEEDBACK_MAX_LENGTH = 2000;

    /** Feedback for the candidate; {@code null} on assessments computed before it existed. */
    @Column(name = "feedback_summary", length = FEEDBACK_MAX_LENGTH)
    private String feedbackSummary;

    @Getter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "assessment_id", nullable = false)
    @OrderBy("id ASC")
    private List<CriterionScore> criterionScores = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "assessment_id", nullable = false)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    private List<IntegrityFlag> integrityFlags = new ArrayList<>();

    private Assessment(InterviewSessionSnapshot session, List<CriterionScore> scores, List<IntegrityFlag> flags,
                       String engineVersion, Instant computedAt) {
        this.interviewSessionId = session.interviewSessionId();
        this.applicationId = session.applicationId();
        this.jobPostingId = session.jobPostingId();
        this.candidateId = session.candidateId();
        this.criterionScores.addAll(scores);
        this.integrityFlags.addAll(flags);
        this.engineVersion = engineVersion.strip();
        this.computedAt = computedAt;
        this.weightedScore = calculateWeightedScore(scores);
    }

    /**
     * @throws BusinessRuleViolationException if the session is not COMPLETED, a criterion is
     *                                        repeated or the weights do not add up to 100
     */
    public static Assessment calculate(InterviewSessionSnapshot session, List<CriterionScore> scores,
                                       List<IntegrityFlag> flags, String engineVersion, Instant computedAt) {
        if (session == null) {
            throw new IllegalArgumentException("Interview session is required");
        }
        if (!session.isCompleted()) {
            throw new BusinessRuleViolationException(
                    "Only a COMPLETED interview can be assessed (current: %s)".formatted(session.status()));
        }
        if (scores == null || scores.isEmpty()) {
            throw new BusinessRuleViolationException("An assessment needs at least one criterion score");
        }
        var criterionIds = new HashSet<Long>();
        if (!scores.stream().allMatch(score -> criterionIds.add(score.getCriterionId()))) {
            throw new BusinessRuleViolationException("Each criterion can be scored only once");
        }
        var totalWeight = scores.stream().mapToInt(CriterionScore::getWeightApplied).sum();
        if (totalWeight != 100) {
            throw new BusinessRuleViolationException(
                    "Applied weights must add up to 100 (current total: %d)".formatted(totalWeight));
        }
        if (engineVersion == null || engineVersion.isBlank()) {
            throw new IllegalArgumentException("Engine version is required");
        }
        if (computedAt == null) {
            throw new IllegalArgumentException("Computation instant is required");
        }
        return new Assessment(session, scores, flags == null ? List.of() : flags, engineVersion, computedAt);
    }

    /** Σ(score × weightApplied) / 100, rounded to one decimal (HALF_UP). */
    public static BigDecimal calculateWeightedScore(List<CriterionScore> scores) {
        return scores.stream()
                .map(CriterionScore::weightedContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(ONE_HUNDRED, 1, RoundingMode.HALF_UP);
    }

    /** Records the candidate feedback, replacing a previous one. */
    public void recordFeedback(String feedbackSummary) {
        if (feedbackSummary == null || feedbackSummary.isBlank()) {
            throw new IllegalArgumentException("Feedback summary is required");
        }
        var stripped = feedbackSummary.strip();
        if (stripped.length() > FEEDBACK_MAX_LENGTH) {
            throw new IllegalArgumentException("Feedback summary must be at most %d characters".formatted(FEEDBACK_MAX_LENGTH));
        }
        this.feedbackSummary = stripped;
    }

    public List<CriterionScore> getCriterionScores() {
        return Collections.unmodifiableList(criterionScores);
    }

    public List<IntegrityFlag> getIntegrityFlags() {
        return Collections.unmodifiableList(integrityFlags);
    }

    public CriterionScore getCriterionScore(Long criterionScoreId) {
        return criterionScores.stream()
                .filter(score -> score.getId() != null && score.getId().equals(criterionScoreId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Criterion score %s not found in assessment %s".formatted(criterionScoreId, getId())));
    }

    public long countEvidences() {
        return criterionScores.stream().mapToLong(score -> score.getEvidences().size()).sum();
    }
}
