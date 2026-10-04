package pe.upc.simutalk.securities;

import pe.upc.simutalk.dtos.GetAssessmentByIdQuery;
import pe.upc.simutalk.services.AssessmentQueryService;
import pe.upc.simutalk.services.IamContextFacade;
import pe.upc.simutalk.services.InterviewsContextFacade;
import pe.upc.simutalk.services.ProfilesContextFacade;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Ownership checks used from {@code @PreAuthorize} as {@code @assessmentAccess}: the
 * authenticated recruiter's company must own the job posting behind the session, the
 * assessment or the ranking. Resolved only through the shared facades.
 */
@Component("assessmentAccess")
@RequiredArgsConstructor
public class AssessmentAccessPolicy {

    private final IamContextFacade iamContextFacade;
    private final ProfilesContextFacade profilesContextFacade;
    private final RecruitmentContextFacade recruitmentContextFacade;
    private final InterviewsContextFacade interviewsContextFacade;
    private final AssessmentQueryService assessmentQueryService;

    public boolean ownsJobPosting(Long jobPostingId, Authentication authentication) {
        return currentCompanyId(authentication)
                .map(companyId -> companyId.equals(recruitmentContextFacade.fetchCompanyIdByJobPostingId(jobPostingId)))
                .orElse(false);
    }

    public boolean ownsSessionJobPosting(Long interviewSessionId, Authentication authentication) {
        return ownsJobPosting(interviewsContextFacade.fetchJobPostingIdBySessionId(interviewSessionId), authentication);
    }

    public boolean ownsAssessment(Long assessmentId, Authentication authentication) {
        return assessmentQueryService.handle(new GetAssessmentByIdQuery(assessmentId))
                .map(assessment -> ownsJobPosting(assessment.getJobPostingId(), authentication))
                .orElse(false);
    }

    /** Whether the authenticated user is the candidate who took this interview. */
    public boolean isSessionCandidate(Long interviewSessionId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        var candidateId = Optional.ofNullable(iamContextFacade.fetchUserIdByUsername(authentication.getName()))
                .filter(id -> id != 0L)
                .map(profilesContextFacade::fetchCandidateIdByUserId)
                .filter(id -> id != null && id != 0L);
        return candidateId.map(id -> id.equals(interviewsContextFacade.fetchCandidateIdBySessionId(interviewSessionId)))
                .orElse(false);
    }

    /** Admins and the recruiter who owns the job posting see the full assessment; everyone else, at most the candidate view. */
    public boolean seesFullAssessment(Long interviewSessionId, Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        var authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return authorities.contains("ROLE_ADMIN")
                || (authorities.contains("ROLE_RECRUITER") && ownsSessionJobPosting(interviewSessionId, authentication));
    }

    private Optional<Long> currentCompanyId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.ofNullable(iamContextFacade.fetchUserIdByUsername(authentication.getName()))
                .filter(id -> id != 0L)
                .map(profilesContextFacade::fetchCompanyIdByUserId)
                .filter(id -> id != null && id != 0L);
    }
}
