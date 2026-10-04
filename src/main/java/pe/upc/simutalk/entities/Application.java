package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.ApplicationStatus;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.InvalidStateTransitionException;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A candidate's application to a job posting. References the job posting and the
 * candidate only by id.
 * <p>
 * Invariants: one can only apply to a PUBLISHED job posting; a candidate applies at most
 * once to the same posting; status changes follow {@link ApplicationStatus}'s directed
 * transitions, and REJECTED and HIRED are final.
 */
@Getter
@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(name = "uk_applications_job_posting_candidate",
                columnNames = {"job_posting_id", "candidate_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends AuditableAggregateRoot<Application> {

    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    /** When the application reached SHORTLISTED (the "terna"); used for time-to-shortlist reports. */
    @Column(name = "shortlisted_at")
    private Instant shortlistedAt;

    private Application(Long jobPostingId, Long candidateId, Instant appliedAt) {
        this.jobPostingId = jobPostingId;
        this.candidateId = candidateId;
        this.status = ApplicationStatus.RECEIVED;
        this.appliedAt = appliedAt;
    }

    /**
     * Creates the application in RECEIVED.
     *
     * @param jobPosting     the posting applied to; only its id is kept
     * @param candidateId    candidate profile id (profiles context)
     * @param alreadyApplied whether this candidate already has an application to the posting
     * @throws BusinessRuleViolationException if the posting is not PUBLISHED or the candidate already applied
     */
    public static Application submit(JobPosting jobPosting, Long candidateId, boolean alreadyApplied, Instant appliedAt) {
        if (jobPosting == null || jobPosting.getId() == null) {
            throw new IllegalArgumentException("A persisted job posting is required");
        }
        if (candidateId == null || candidateId <= 0) {
            throw new IllegalArgumentException("Candidate id must be a positive number");
        }
        if (appliedAt == null) {
            throw new IllegalArgumentException("Application instant is required");
        }
        if (!jobPosting.isPublished()) {
            throw new BusinessRuleViolationException(
                    "Applications are only accepted for PUBLISHED job postings (current status: %s)"
                            .formatted(jobPosting.getStatus()));
        }
        if (alreadyApplied) {
            throw new BusinessRuleViolationException("The candidate already applied to this job posting");
        }
        return new Application(jobPosting.getId(), candidateId, appliedAt);
    }

    /**
     * @throws InvalidStateTransitionException (an IllegalStateException) if the transition is not allowed
     */
    public void changeStatus(ApplicationStatus target) {
        changeStatus(target, Instant.now());
    }

    /**
     * @param changedAt when the change happens; stored as {@code shortlistedAt} when moving to SHORTLISTED
     */
    public void changeStatus(ApplicationStatus target, Instant changedAt) {
        if (target == null) {
            throw new IllegalArgumentException("Target status is required");
        }
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("An application cannot move from %s to %s (allowed: %s)"
                    .formatted(status, target, status.allowedNextStatuses()));
        }
        this.status = target;
        if (target == ApplicationStatus.SHORTLISTED) {
            this.shortlistedAt = changedAt;
        }
    }

    public boolean belongsToCandidate(Long otherCandidateId) {
        return candidateId.equals(otherCandidateId);
    }
}
