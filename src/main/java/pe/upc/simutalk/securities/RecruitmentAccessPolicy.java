package pe.upc.simutalk.securities;

import pe.upc.simutalk.dtos.GetApplicationByIdQuery;
import pe.upc.simutalk.dtos.GetJobPostingByIdQuery;
import pe.upc.simutalk.dtos.JobPostingViewer;
import pe.upc.simutalk.services.ApplicationQueryService;
import pe.upc.simutalk.services.IamContextFacade;
import pe.upc.simutalk.services.JobPostingQueryService;
import pe.upc.simutalk.services.ProfilesContextFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Ownership checks used from {@code @PreAuthorize} as {@code @recruitmentAccess}, modeled
 * after profiles' ProfileAccessPolicy. The company of the authenticated user is resolved
 * through the IamContextFacade and ProfilesContextFacade contracts in shared, so
 * recruitment never imports iam or profiles classes.
 */
@Component("recruitmentAccess")
@RequiredArgsConstructor
public class RecruitmentAccessPolicy {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final IamContextFacade iamContextFacade;
    private final ProfilesContextFacade profilesContextFacade;
    private final JobPostingQueryService jobPostingQueryService;
    private final ApplicationQueryService applicationQueryService;

    /** True when the authenticated user's company owns the job posting. */
    public boolean ownsJobPosting(Long jobPostingId, Authentication authentication) {
        return currentCompanyId(authentication)
                .map(companyId -> jobPostingQueryService
                        .handle(new GetJobPostingByIdQuery(jobPostingId, JobPostingViewer.unrestrictedViewer()))
                        .map(jobPosting -> jobPosting.isOwnedBy(companyId))
                        .orElse(false))
                .orElse(false);
    }

    /** True when the authenticated user's company owns the job posting the application belongs to. */
    public boolean ownsApplicationJobPosting(Long applicationId, Authentication authentication) {
        return applicationQueryService.handle(new GetApplicationByIdQuery(applicationId))
                .map(application -> ownsJobPosting(application.getJobPostingId(), authentication))
                .orElse(false);
    }

    /** Company profile id of the authenticated user, if it has one. */
    public Optional<Long> currentCompanyId(Authentication authentication) {
        return currentUserId(authentication)
                .map(profilesContextFacade::fetchCompanyIdByUserId)
                .filter(RecruitmentAccessPolicy::isPresentId);
    }

    /** Candidate profile id of the authenticated user, if it has one. */
    public Optional<Long> currentCandidateId(Authentication authentication) {
        return currentUserId(authentication)
                .map(profilesContextFacade::fetchCandidateIdByUserId)
                .filter(RecruitmentAccessPolicy::isPresentId);
    }

    private Optional<Long> currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.ofNullable(iamContextFacade.fetchUserIdByUsername(authentication.getName()))
                .filter(RecruitmentAccessPolicy::isPresentId);
    }

    /** The facades answer 0 when there is no such id. */
    private static boolean isPresentId(Long id) {
        return id != null && id != 0L;
    }

    /** Visibility context for reads: admins see everything, others see by company. */
    public JobPostingViewer currentViewer(Authentication authentication) {
        var isAdmin = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_ADMIN::equals);
        return isAdmin
                ? JobPostingViewer.unrestrictedViewer()
                : JobPostingViewer.ofCompany(currentCompanyId(authentication).orElse(null));
    }
}
