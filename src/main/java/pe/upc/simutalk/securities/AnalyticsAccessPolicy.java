package pe.upc.simutalk.securities;

import pe.upc.simutalk.services.IamContextFacade;
import pe.upc.simutalk.services.ProfilesContextFacade;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Ownership checks used from {@code @PreAuthorize} as {@code @analyticsAccess}: reports belong
 * to the recruiter of the company that owns the job posting (or the company itself).
 */
@Component("analyticsAccess")
@RequiredArgsConstructor
public class AnalyticsAccessPolicy {

    private final IamContextFacade iamContextFacade;
    private final ProfilesContextFacade profilesContextFacade;
    private final RecruitmentContextFacade recruitmentContextFacade;

    public boolean ownsJobPosting(Long jobPostingId, Authentication authentication) {
        return currentCompanyId(authentication)
                .map(companyId -> companyId.equals(recruitmentContextFacade.fetchCompanyIdByJobPostingId(jobPostingId)))
                .orElse(false);
    }

    public boolean isCompany(Long companyId, Authentication authentication) {
        return currentCompanyId(authentication).map(current -> current.equals(companyId)).orElse(false);
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
