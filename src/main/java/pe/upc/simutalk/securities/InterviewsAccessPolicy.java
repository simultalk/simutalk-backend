package pe.upc.simutalk.securities;

import pe.upc.simutalk.dtos.GetInterviewSessionByIdQuery;
import pe.upc.simutalk.dtos.HasInterviewSessionInProgressQuery;
import pe.upc.simutalk.entities.InterviewSession;
import pe.upc.simutalk.services.IamContextFacade;
import pe.upc.simutalk.services.InterviewSessionQueryService;
import pe.upc.simutalk.services.ProfilesContextFacade;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Ownership checks used from {@code @PreAuthorize} as {@code @interviewsAccess}, modeled after
 * ProfileAccessPolicy and RecruitmentAccessPolicy. The authenticated user's company and
 * candidate profile are resolved through the shared facades; interviews never imports
 * iam, profiles or recruitment classes.
 */
@Component("interviewsAccess")
@RequiredArgsConstructor
public class InterviewsAccessPolicy {

    private final IamContextFacade iamContextFacade;
    private final ProfilesContextFacade profilesContextFacade;
    private final RecruitmentContextFacade recruitmentContextFacade;
    private final InterviewSessionQueryService interviewSessionQueryService;

    /** The authenticated recruiter's company owns the job posting. */
    public boolean ownsJobPosting(Long jobPostingId, Authentication authentication) {
        return currentCompanyId(authentication)
                .map(companyId -> companyId.equals(recruitmentContextFacade.fetchCompanyIdByJobPostingId(jobPostingId)))
                .orElse(false);
    }

    /** Owner of the job posting, or a candidate with an IN_PROGRESS interview for it. */
    public boolean canReadScript(Long jobPostingId, Authentication authentication) {
        return ownsJobPosting(jobPostingId, authentication) || currentCandidateId(authentication)
                .map(candidateId -> interviewSessionQueryService.handle(
                        new HasInterviewSessionInProgressQuery(jobPostingId, candidateId)))
                .orElse(false);
    }

    public boolean ownsApplicationJobPosting(Long applicationId, Authentication authentication) {
        return ownsJobPosting(recruitmentContextFacade.fetchJobPostingIdByApplicationId(applicationId), authentication);
    }

    public boolean isApplicationCandidate(Long applicationId, Authentication authentication) {
        return currentCandidateId(authentication)
                .map(candidateId -> candidateId.equals(recruitmentContextFacade.fetchCandidateIdByApplicationId(applicationId)))
                .orElse(false);
    }

    public boolean isSessionCandidate(Long interviewSessionId, Authentication authentication) {
        return currentCandidateId(authentication)
                .map(candidateId -> findSession(interviewSessionId, session -> session.belongsToCandidate(candidateId)))
                .orElse(false);
    }

    public boolean ownsSessionJobPosting(Long interviewSessionId, Authentication authentication) {
        return interviewSessionQueryService.handle(new GetInterviewSessionByIdQuery(interviewSessionId))
                .map(session -> ownsJobPosting(session.getJobPostingId(), authentication))
                .orElse(false);
    }

    private boolean findSession(Long interviewSessionId, Predicate<InterviewSession> condition) {
        return interviewSessionQueryService.handle(new GetInterviewSessionByIdQuery(interviewSessionId))
                .map(condition::test)
                .orElse(false);
    }

    private Optional<Long> currentCompanyId(Authentication authentication) {
        return currentUserId(authentication).map(profilesContextFacade::fetchCompanyIdByUserId).filter(InterviewsAccessPolicy::isPresentId);
    }

    private Optional<Long> currentCandidateId(Authentication authentication) {
        return currentUserId(authentication).map(profilesContextFacade::fetchCandidateIdByUserId).filter(InterviewsAccessPolicy::isPresentId);
    }

    private Optional<Long> currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.ofNullable(iamContextFacade.fetchUserIdByUsername(authentication.getName()))
                .filter(InterviewsAccessPolicy::isPresentId);
    }

    /** The facades answer 0 when there is no such id. */
    private static boolean isPresentId(Long id) {
        return id != null && id != 0L;
    }
}
