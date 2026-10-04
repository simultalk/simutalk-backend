package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.QuestionOrigin;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A question of a job posting's interview script. References the job posting and the
 * criterion it evaluates only by id (both live in recruitment).
 * <p>
 * Invariants: the statement is not blank (max 500 characters), the answer time is between
 * 30 and 600 seconds and the position is 1 or greater. Rules that need recruitment (DRAFT
 * posting, criterion of the same posting and of type COMPETENCY) are checked by the
 * application service through the ACL.
 */
@Getter
@Entity
@Table(name = "questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends AuditableAggregateRoot<Question> {

    public static final int STATEMENT_MAX_LENGTH = 500;
    public static final int MIN_DURATION_SECONDS = 30;
    public static final int MAX_DURATION_SECONDS = 600;

    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;

    @Column(name = "criterion_id", nullable = false)
    private Long criterionId;

    @Column(nullable = false, length = STATEMENT_MAX_LENGTH)
    private String statement;

    @Column(name = "max_duration_seconds", nullable = false)
    private int maxDurationSeconds;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionOrigin origin;

    @Column(name = "allows_follow_up", nullable = false)
    private boolean allowsFollowUp;

    public Question(Long jobPostingId, Long criterionId, String statement, int maxDurationSeconds, int position,
                    QuestionOrigin origin, boolean allowsFollowUp) {
        if (jobPostingId == null || jobPostingId <= 0) {
            throw new IllegalArgumentException("Job posting id must be a positive number");
        }
        this.jobPostingId = jobPostingId;
        moveTo(position);
        applyDetails(criterionId, statement, maxDurationSeconds, origin, allowsFollowUp);
    }

    public void updateDetails(Long criterionId, String statement, int maxDurationSeconds, QuestionOrigin origin,
                              boolean allowsFollowUp) {
        applyDetails(criterionId, statement, maxDurationSeconds, origin, allowsFollowUp);
    }

    /** Sets the position inside the script (1-based). */
    public void moveTo(int position) {
        if (position < 1) {
            throw new IllegalArgumentException("Question position must be 1 or greater");
        }
        this.position = position;
    }

    public boolean belongsTo(Long otherJobPostingId) {
        return jobPostingId.equals(otherJobPostingId);
    }

    private void applyDetails(Long criterionId, String statement, int maxDurationSeconds, QuestionOrigin origin,
                              boolean allowsFollowUp) {
        if (criterionId == null || criterionId <= 0) {
            throw new IllegalArgumentException("Criterion id must be a positive number");
        }
        if (statement == null || statement.isBlank()) {
            throw new IllegalArgumentException("Question statement is required");
        }
        var stripped = statement.strip();
        if (stripped.length() > STATEMENT_MAX_LENGTH) {
            throw new IllegalArgumentException("Question statement must be at most %d characters"
                    .formatted(STATEMENT_MAX_LENGTH));
        }
        if (maxDurationSeconds < MIN_DURATION_SECONDS || maxDurationSeconds > MAX_DURATION_SECONDS) {
            throw new IllegalArgumentException("Question max duration must be between %d and %d seconds"
                    .formatted(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS));
        }
        if (origin == null) {
            throw new IllegalArgumentException("Question origin is required");
        }
        this.criterionId = criterionId;
        this.statement = stripped;
        this.maxDurationSeconds = maxDurationSeconds;
        this.origin = origin;
        this.allowsFollowUp = allowsFollowUp;
    }
}
