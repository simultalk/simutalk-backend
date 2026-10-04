package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CriterionView;

import java.util.List;

/**
 * Contract other bounded contexts (interviews, assessment) use to ask recruitment about job
 * postings and applications. Implemented by recruitment. Missing ids answer neutral values
 * ({@code false}, {@code 0L}, empty list or empty string) instead of throwing.
 */
public interface RecruitmentContextFacade {

    boolean existsJobPostingById(Long jobPostingId);

    boolean isJobPostingPublished(Long jobPostingId);

    /** Whether the job posting exists and is still in DRAFT (its script and weights can change). */
    boolean isJobPostingDraft(Long jobPostingId);

    /** @return the owning company profile id, or {@code 0L} */
    Long fetchCompanyIdByJobPostingId(Long jobPostingId);

    /** @return the job posting title, or an empty string if the posting does not exist */
    String fetchJobPostingTitle(Long jobPostingId);

    /** @return the job posting description, or an empty string if the posting does not exist */
    String fetchJobPostingDescription(Long jobPostingId);

    boolean existsCriterionInJobPosting(Long jobPostingId, Long criterionId);

    /** Every criterion of the job posting, in creation order; empty if the posting does not exist. */
    List<CriterionView> fetchCriteria(Long jobPostingId);

    /** Whether the job posting hides candidates' personal data from human evaluators. */
    boolean isAnonymizedScreening(Long jobPostingId);

    /** Ids of the job posting's COMPETENCY criteria (CERTIFICATION ones are not interviewed). */
    List<Long> fetchCompetencyCriterionIds(Long jobPostingId);

    /** @return the job posting id of the application, or {@code 0L} */
    Long fetchJobPostingIdByApplicationId(Long applicationId);

    /** @return ids of the job posting's applications in the order they were submitted; empty if there are none */
    List<Long> fetchApplicationIds(Long jobPostingId);

    /** @return the candidate profile id of the application, or {@code 0L} */
    Long fetchCandidateIdByApplicationId(Long applicationId);

    /** @return the ApplicationStatus name, or an empty string if the application does not exist */
    String fetchApplicationStatus(Long applicationId);

    /** Applications per pipeline stage (every ApplicationStatus name, 0 included). */
    java.util.Map<String, Long> countApplicationsByStatus(Long jobPostingId);

    long countPublishedJobPostingsByCompanyId(Long companyId);

    List<Long> fetchJobPostingIdsByCompanyId(Long companyId);

    /** Average seconds from applying to SHORTLISTED over the given postings, or {@code null} if nobody was shortlisted. */
    Long fetchAverageSecondsToShortlist(List<Long> jobPostingIds);

    /** Moves the application to INTERVIEWING through its aggregate (transition rules apply). */
    void markApplicationAsInterviewing(Long applicationId);

    /** Moves the application to ASSESSED through its aggregate (transition rules apply). */
    void markApplicationAsAssessed(Long applicationId);
}
