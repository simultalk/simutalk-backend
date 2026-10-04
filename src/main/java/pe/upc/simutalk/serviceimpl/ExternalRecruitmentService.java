package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Anti-corruption layer from interviews to recruitment, through the RecruitmentContextFacade
 * contract in shared. Translates recruitment's neutral answers into interviews' errors.
 */
@Service
@RequiredArgsConstructor
public class ExternalRecruitmentService {

    static final String APPLICATION_RECEIVED = "RECEIVED";

    private final RecruitmentContextFacade recruitmentContextFacade;

    /**
     * The script of a job posting is editable only while the posting is DRAFT; once
     * published it is frozen, like the weights.
     */
    public void ensureScriptIsEditable(Long jobPostingId) {
        if (!recruitmentContextFacade.existsJobPostingById(jobPostingId)) {
            throw new ResourceNotFoundException("Job posting", jobPostingId);
        }
        if (!recruitmentContextFacade.isJobPostingDraft(jobPostingId)) {
            throw new BusinessRuleViolationException(
                    "The interview script can only change while the job posting is in DRAFT");
        }
    }

    /**
     * The criterion must belong to the job posting and be a COMPETENCY: CERTIFICATION criteria
     * are not evaluated by interview.
     */
    public void ensureCompetencyCriterion(Long jobPostingId, Long criterionId) {
        List<Long> competencyIds = recruitmentContextFacade.fetchCompetencyCriterionIds(jobPostingId);
        if (competencyIds.contains(criterionId)) {
            return;
        }
        if (recruitmentContextFacade.existsCriterionInJobPosting(jobPostingId, criterionId)) {
            throw new BusinessRuleViolationException(
                    "Criterion %s is a CERTIFICATION criterion: certifications are not evaluated by interview, only COMPETENCY criteria can have questions"
                            .formatted(criterionId));
        }
        throw new BusinessRuleViolationException(
                "Criterion %s does not belong to job posting %s".formatted(criterionId, jobPostingId));
    }

    /**
     * Questions can only be suggested while they can still be added: suggestions exist to be accepted
     * into the script, and the script of a published job posting is frozen.
     */
    public void ensureQuestionsCanBeSuggested(Long jobPostingId) {
        if (!recruitmentContextFacade.existsJobPostingById(jobPostingId)) {
            throw new ResourceNotFoundException("Job posting", jobPostingId);
        }
        if (!recruitmentContextFacade.isJobPostingDraft(jobPostingId)) {
            throw new BusinessRuleViolationException(
                    "Questions can only be suggested while the job posting is in DRAFT");
        }
    }

    /**
     * The job posting's and the criterion's own text, which is all the question suggestion port may
     * know. The criterion must be a COMPETENCY of the job posting (same rule as adding a question).
     */
    public CriterionContext fetchCompetencyCriterionContext(Long jobPostingId, Long criterionId) {
        ensureCompetencyCriterion(jobPostingId, criterionId);
        var criterion = recruitmentContextFacade.fetchCriteria(jobPostingId).stream()
                .filter(view -> criterionId.equals(view.criterionId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Criterion %s not found in job posting %s".formatted(criterionId, jobPostingId)));
        return new CriterionContext(recruitmentContextFacade.fetchJobPostingTitle(jobPostingId),
                recruitmentContextFacade.fetchJobPostingDescription(jobPostingId), criterion.name(),
                criterion.description());
    }

    /** @return the job posting and candidate of an application that is still RECEIVED */
    public ApplicationSnapshot fetchApplicationReadyForInterview(Long applicationId) {
        var status = recruitmentContextFacade.fetchApplicationStatus(applicationId);
        if (status.isEmpty()) {
            throw new ResourceNotFoundException("Application", applicationId);
        }
        if (!APPLICATION_RECEIVED.equals(status)) {
            throw new BusinessRuleViolationException(
                    "An interview can only be created for a RECEIVED application (current: %s)".formatted(status));
        }
        return new ApplicationSnapshot(applicationId,
                recruitmentContextFacade.fetchJobPostingIdByApplicationId(applicationId),
                recruitmentContextFacade.fetchCandidateIdByApplicationId(applicationId));
    }

    public void markApplicationAsInterviewing(Long applicationId) {
        recruitmentContextFacade.markApplicationAsInterviewing(applicationId);
    }

    public void markApplicationAsAssessed(Long applicationId) {
        recruitmentContextFacade.markApplicationAsAssessed(applicationId);
    }

    public record ApplicationSnapshot(Long applicationId, Long jobPostingId, Long candidateId) {
    }

    public record CriterionContext(String jobTitle, String jobDescription, String criterionName,
                                   String criterionDescription) {
    }
}
