package pe.upc.simutalk.entities;

import pe.upc.simutalk.dtos.CreateJobPostingCommand;
import pe.upc.simutalk.dtos.JobPostingViewer;
import pe.upc.simutalk.enums.CriterionOrigin;
import pe.upc.simutalk.enums.CriterionType;
import pe.upc.simutalk.enums.JobPostingStatus;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.services.InterviewQuestionCounter;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A vacancy published by a company, together with the weighted criteria its
 * candidates are evaluated against.
 * <p>
 * Invariants guarded here:
 * <ul>
 *   <li>A job posting can only be published when it has at least one criterion, the
 *       weights add up to exactly 100 and every COMPETENCY criterion has at least one
 *       interview question.</li>
 *   <li>Criteria can only be added, changed or removed while the posting is DRAFT,
 *       so every candidate is scored against the same weights.</li>
 *   <li>Criterion names are unique within a posting (case-insensitive).</li>
 *   <li>A CLOSED posting is read-only; the anonymized screening flag is fixed once
 *       the posting leaves DRAFT.</li>
 *   <li>A PUBLISHED posting cannot be deleted; it has to be closed first.</li>
 *   <li>A DRAFT is only visible to its own company; lists show other companies'
 *       postings only while PUBLISHED.</li>
 * </ul>
 */
@Getter
@Entity
@Table(name = "job_postings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPosting extends AuditableAggregateRoot<JobPosting> {

    public static final int TITLE_MAX_LENGTH = 150;
    public static final int DESCRIPTION_MAX_LENGTH = 4000;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false, length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Embedded
    private CompanyId companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobPostingStatus status;

    @Column(name = "closing_date")
    private LocalDate closingDate;

    @Column(name = "anonymized_screening", nullable = false)
    private boolean anonymizedScreening;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "jobPosting", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<EvaluationCriterion> criteria = new ArrayList<>();

    public JobPosting(CreateJobPostingCommand command) {
        this.companyId = new CompanyId(command.companyId());
        this.status = JobPostingStatus.DRAFT;
        this.title = requireText(command.title(), "title", TITLE_MAX_LENGTH);
        this.description = requireText(command.description(), "description", DESCRIPTION_MAX_LENGTH);
        this.closingDate = command.closingDate();
        this.anonymizedScreening = command.anonymizedScreening();
    }

    public List<EvaluationCriterion> getCriteria() {
        return Collections.unmodifiableList(criteria);
    }

    public int getTotalWeight() {
        return criteria.stream().mapToInt(criterion -> criterion.getWeight().value()).sum();
    }

    public boolean isDraft() {
        return status == JobPostingStatus.DRAFT;
    }

    public boolean isPublished() {
        return status == JobPostingStatus.PUBLISHED;
    }

    public boolean isOwnedBy(Long otherCompanyId) {
        return otherCompanyId != null && companyId.value().equals(otherCompanyId);
    }

    /** Whether the viewer may open this posting by id: DRAFTs only for the owning company. */
    public boolean isVisibleTo(JobPostingViewer viewer) {
        return viewer.unrestricted() || !isDraft() || isOwnedBy(viewer.companyId());
    }

    /** Whether the posting appears in the viewer's listings: others only see PUBLISHED ones. */
    public boolean isListedFor(JobPostingViewer viewer) {
        return viewer.unrestricted() || status == JobPostingStatus.PUBLISHED || isOwnedBy(viewer.companyId());
    }

    /* ---------- details ---------- */

    public void updateDetails(String title, String description, LocalDate closingDate, boolean anonymizedScreening) {
        if (status == JobPostingStatus.CLOSED) {
            throw new BusinessRuleViolationException("A closed job posting cannot be modified");
        }
        if (!isDraft() && anonymizedScreening != this.anonymizedScreening) {
            throw new BusinessRuleViolationException(
                    "Anonymized screening can only be changed while the job posting is in DRAFT");
        }
        this.title = requireText(title, "title", TITLE_MAX_LENGTH);
        this.description = requireText(description, "description", DESCRIPTION_MAX_LENGTH);
        this.closingDate = closingDate;
        this.anonymizedScreening = anonymizedScreening;
    }

    /* ---------- criteria ---------- */

    public EvaluationCriterion addCriterion(String name, String description, Weight weight,
                                            CriterionType criterionType, String certificationName, boolean mandatory) {
        return addCriterion(name, description, weight, criterionType, certificationName, mandatory, CriterionOrigin.MANUAL);
    }

    /** Adds a criterion; an {@code AI_SUGGESTED} one still needs the weight the recruiter chose. */
    public EvaluationCriterion addCriterion(String name, String description, Weight weight, CriterionType criterionType,
                                            String certificationName, boolean mandatory, CriterionOrigin origin) {
        ensureCriteriaAreEditable();
        ensureUniqueCriterionName(name, null);
        var criterion = new EvaluationCriterion(this, name, description, weight, criterionType, certificationName,
                mandatory, origin);
        criteria.add(criterion);
        return criterion;
    }

    /**
     * Criteria can only be suggested while they can still be edited: suggestions exist to be accepted
     * into the posting, and a published posting's criteria are frozen.
     */
    public void ensureCriteriaCanBeSuggested() {
        if (!isDraft()) {
            throw new BusinessRuleViolationException(
                    "Criteria can only be suggested while the job posting is in DRAFT (current status: %s)".formatted(status));
        }
    }

    /** Names of the criteria already defined, so suggestions do not repeat them. */
    public List<String> getCriterionNames() {
        return criteria.stream().map(EvaluationCriterion::getName).toList();
    }

    public EvaluationCriterion updateCriterion(Long criterionId, String name, String description, Weight weight,
                                               CriterionType criterionType, String certificationName, boolean mandatory) {
        ensureCriteriaAreEditable();
        var criterion = findCriterion(criterionId);
        ensureUniqueCriterionName(name, criterionId);
        criterion.update(name, description, weight, criterionType, certificationName, mandatory);
        return criterion;
    }

    public void removeCriterion(Long criterionId) {
        ensureCriteriaAreEditable();
        criteria.remove(findCriterion(criterionId));
    }

    /* ---------- lifecycle ---------- */

    /**
     * Moves the posting from DRAFT to PUBLISHED.
     *
     * @param questionCounter how many interview questions evaluate each criterion
     * @throws BusinessRuleViolationException if the posting is not DRAFT, has no criteria,
     *                                        its weights do not add up to 100, or a
     *                                        COMPETENCY criterion has no interview question
     */
    public void publish(InterviewQuestionCounter questionCounter) {
        if (questionCounter == null) {
            throw new IllegalArgumentException("An interview question counter is required to publish");
        }
        if (!isDraft()) {
            throw new BusinessRuleViolationException(
                    "Only a DRAFT job posting can be published (current status: %s)".formatted(status));
        }
        if (criteria.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "A job posting needs at least one evaluation criterion to be published");
        }
        var totalWeight = getTotalWeight();
        if (totalWeight != Weight.TOTAL) {
            throw new BusinessRuleViolationException(
                    "The weights of the evaluation criteria must add up to exactly %d (current total: %d)"
                            .formatted(Weight.TOTAL, totalWeight));
        }
        var competenciesWithoutQuestions = criteria.stream()
                .filter(criterion -> criterion.getCriterionType() == CriterionType.COMPETENCY)
                .filter(criterion -> questionCounter.countQuestionsByCriterionId(criterion.getId()) == 0)
                .map(EvaluationCriterion::getName)
                .toList();
        if (!competenciesWithoutQuestions.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Every COMPETENCY criterion needs at least one interview question before publishing; missing: %s"
                            .formatted(competenciesWithoutQuestions));
        }
        this.status = JobPostingStatus.PUBLISHED;
    }

    public void close() {
        if (status == JobPostingStatus.CLOSED) {
            throw new BusinessRuleViolationException("The job posting is already closed");
        }
        this.status = JobPostingStatus.CLOSED;
    }

    public void changeStatus(JobPostingStatus targetStatus, InterviewQuestionCounter questionCounter) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("Target status is required");
        }
        switch (targetStatus) {
            case PUBLISHED -> publish(questionCounter);
            case CLOSED -> close();
            case DRAFT -> throw new BusinessRuleViolationException("A job posting cannot go back to DRAFT");
        }
    }

    public void ensureCanBeDeleted() {
        if (status == JobPostingStatus.PUBLISHED) {
            throw new BusinessRuleViolationException("A published job posting cannot be deleted; close it first");
        }
    }

    /* ---------- helpers ---------- */

    private void ensureCriteriaAreEditable() {
        if (!isDraft()) {
            throw new BusinessRuleViolationException(
                    "Evaluation criteria can only be modified while the job posting is in DRAFT (current status: %s)"
                            .formatted(status));
        }
    }

    private void ensureUniqueCriterionName(String name, Long excludedCriterionId) {
        var duplicated = criteria.stream()
                .filter(criterion -> excludedCriterionId == null || !excludedCriterionId.equals(criterion.getId()))
                .anyMatch(criterion -> criterion.hasName(name));
        if (duplicated) {
            throw new BusinessRuleViolationException(
                    "An evaluation criterion named '%s' already exists in this job posting".formatted(name.strip()));
        }
    }

    private EvaluationCriterion findCriterion(Long criterionId) {
        return criteria.stream()
                .filter(criterion -> criterion.getId() != null && criterion.getId().equals(criterionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evaluation criterion %s not found in job posting %s".formatted(criterionId, getId())));
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Job posting %s is required".formatted(field));
        }
        var stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new IllegalArgumentException("Job posting %s must be at most %d characters".formatted(field, maxLength));
        }
        return stripped;
    }
}
