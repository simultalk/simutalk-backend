package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.InterviewSessionStatus;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.InvalidStateTransitionException;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The asynchronous interview of one application (one session per application). Records what
 * was asked and answered; it never scores.
 * <p>
 * {@code jobPostingId} and {@code candidateId} are immutable copies taken from the
 * application when the session is created, used for authorization lookups.
 * <p>
 * Lifecycle: PENDING -> IN_PROGRESS -> COMPLETED, and PENDING / IN_PROGRESS -> EXPIRED once
 * {@code expiresAt} has passed. COMPLETED and EXPIRED are final.
 */
@Getter
@Entity
@Table(name = "interview_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSession extends AuditableAggregateRoot<InterviewSession> {

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewSessionStatus status;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Column(name = "total_duration_seconds", nullable = false)
    private int totalDurationSeconds;

    @Getter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "interview_session_id", nullable = false)
    @OrderBy("id ASC")
    private List<Answer> answers = new ArrayList<>();

    public InterviewSession(Long applicationId, Long jobPostingId, Long candidateId, LocalDate expiresAt,
                            Instant invitedAt) {
        requirePositive(applicationId, "Application id");
        requirePositive(jobPostingId, "Job posting id");
        requirePositive(candidateId, "Candidate id");
        if (invitedAt == null) {
            throw new IllegalArgumentException("Invitation instant is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiration date is required");
        }
        if (expiresAt.isBefore(LocalDate.ofInstant(invitedAt, ZoneOffset.UTC))) {
            throw new IllegalArgumentException("The expiration date cannot be before the invitation date");
        }
        this.applicationId = applicationId;
        this.jobPostingId = jobPostingId;
        this.candidateId = candidateId;
        this.expiresAt = expiresAt;
        this.invitedAt = invitedAt;
        this.status = InterviewSessionStatus.PENDING;
        this.totalDurationSeconds = 0;
    }

    public List<Answer> getAnswers() {
        return Collections.unmodifiableList(answers);
    }

    public boolean isPastDue(LocalDate today) {
        return today.isAfter(expiresAt);
    }

    public boolean belongsToCandidate(Long otherCandidateId) {
        return candidateId.equals(otherCandidateId);
    }

    /* ---------- lifecycle ---------- */

    /**
     * @throws InvalidStateTransitionException if the session is not PENDING or is past due
     */
    public void start(LocalDate today, Instant now) {
        if (status != InterviewSessionStatus.PENDING) {
            throw new InvalidStateTransitionException("Only a PENDING session can be started (current: %s)".formatted(status));
        }
        if (isPastDue(today)) {
            throw new InvalidStateTransitionException("The session expired on %s and can no longer be started".formatted(expiresAt));
        }
        this.status = InterviewSessionStatus.IN_PROGRESS;
        this.startedAt = now;
    }

    /**
     * Records an answer to {@code question}.
     * <ul>
     *   <li>Only while IN_PROGRESS.</li>
     *   <li>A question gets a single regular answer.</li>
     *   <li>A follow-up needs its parent answer in this session, to the same question, a
     *       question that allows follow-ups, and at most one follow-up per question.</li>
     * </ul>
     *
     * @throws InvalidStateTransitionException if the session is not IN_PROGRESS
     * @throws BusinessRuleViolationException  if a rule above is broken
     */
    public Answer recordAnswer(Answer answer, Question question) {
        if (answer == null || question == null) {
            throw new IllegalArgumentException("Answer and question are required");
        }
        if (status != InterviewSessionStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Answers are only accepted while the session is IN_PROGRESS (current: %s)"
                    .formatted(status));
        }
        if (!question.belongsTo(jobPostingId) || !answer.getQuestionId().equals(question.getId())) {
            throw new BusinessRuleViolationException("The question does not belong to this interview's script");
        }
        if (answer.isFollowUp()) {
            ensureFollowUpIsAllowed(answer, question);
        } else if (hasRegularAnswerFor(answer.getQuestionId())) {
            throw new BusinessRuleViolationException("Question %s has already been answered".formatted(answer.getQuestionId()));
        }
        answers.add(answer);
        return answer;
    }

    /**
     * @param expectedQuestionCount number of questions in the job posting's script
     * @throws InvalidStateTransitionException if the session is not IN_PROGRESS
     * @throws BusinessRuleViolationException  if some script question has no answer
     */
    public void complete(int expectedQuestionCount, Instant now) {
        if (status != InterviewSessionStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Only an IN_PROGRESS session can be completed (current: %s)".formatted(status));
        }
        var answeredQuestions = answeredQuestionCount();
        if (answeredQuestions < expectedQuestionCount) {
            throw new BusinessRuleViolationException("%d of %d questions are still unanswered"
                    .formatted(expectedQuestionCount - answeredQuestions, expectedQuestionCount));
        }
        this.status = InterviewSessionStatus.COMPLETED;
        this.finishedAt = now;
        this.totalDurationSeconds = answers.stream().mapToInt(Answer::getDurationSeconds).sum();
    }

    /**
     * @throws InvalidStateTransitionException if the session is final or not yet past due
     */
    public void expire(LocalDate today) {
        if (status.isFinal()) {
            throw new InvalidStateTransitionException("A %s session cannot expire".formatted(status));
        }
        if (!isPastDue(today)) {
            throw new InvalidStateTransitionException("The session is valid until %s".formatted(expiresAt));
        }
        this.status = InterviewSessionStatus.EXPIRED;
    }

    /** Distinct script questions with a regular (non follow-up) answer. */
    public long answeredQuestionCount() {
        return answers.stream().filter(answer -> !answer.isFollowUp()).map(Answer::getQuestionId).distinct().count();
    }

    /* ---------- helpers ---------- */

    private void ensureFollowUpIsAllowed(Answer followUp, Question question) {
        if (!question.isAllowsFollowUp()) {
            throw new BusinessRuleViolationException("Question %s does not allow follow-up questions".formatted(question.getId()));
        }
        var parent = answers.stream()
                .filter(existing -> existing.getId() != null && existing.getId().equals(followUp.getParentAnswerId()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "The parent answer %s does not exist in this session".formatted(followUp.getParentAnswerId())));
        if (parent.isFollowUp() || !parent.getQuestionId().equals(followUp.getQuestionId())) {
            throw new BusinessRuleViolationException("A follow-up must reply to the regular answer of the same question");
        }
        var alreadyHasFollowUp = answers.stream()
                .anyMatch(existing -> existing.isFollowUp() && existing.getQuestionId().equals(followUp.getQuestionId()));
        if (alreadyHasFollowUp) {
            throw new BusinessRuleViolationException("Question %s already has a follow-up".formatted(followUp.getQuestionId()));
        }
    }

    private boolean hasRegularAnswerFor(Long questionId) {
        return answers.stream().anyMatch(existing -> !existing.isFollowUp() && existing.getQuestionId().equals(questionId));
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be a positive number");
        }
    }
}
