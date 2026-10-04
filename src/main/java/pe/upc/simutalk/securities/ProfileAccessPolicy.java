package pe.upc.simutalk.securities;

import pe.upc.simutalk.dtos.GetCandidateProfileByIdQuery;
import pe.upc.simutalk.dtos.GetCompanyProfileByIdQuery;
import pe.upc.simutalk.services.CandidateProfileQueryService;
import pe.upc.simutalk.services.CompanyProfileQueryService;
import pe.upc.simutalk.services.IamContextFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Ownership checks used from {@code @PreAuthorize} as {@code @profileAccess}. The current
 * user id is resolved through the IamContextFacade contract, so profiles never imports
 * iam classes.
 */
@Component("profileAccess")
@RequiredArgsConstructor
public class ProfileAccessPolicy {

    private final IamContextFacade iamContextFacade;
    private final CandidateProfileQueryService candidateProfileQueryService;
    private final CompanyProfileQueryService companyProfileQueryService;

    public boolean isCurrentUser(Long userId, Authentication authentication) {
        var currentUserId = currentUserId(authentication);
        return currentUserId != null && currentUserId.equals(userId);
    }

    public boolean ownsCandidate(Long candidateId, Authentication authentication) {
        var currentUserId = currentUserId(authentication);
        return currentUserId != null && candidateProfileQueryService.handle(new GetCandidateProfileByIdQuery(candidateId))
                .map(candidate -> candidate.isOwnedBy(currentUserId))
                .orElse(false);
    }

    public boolean ownsCompany(Long companyProfileId, Authentication authentication) {
        var currentUserId = currentUserId(authentication);
        return currentUserId != null && companyProfileQueryService.handle(new GetCompanyProfileByIdQuery(companyProfileId))
                .map(company -> company.isOwnedBy(currentUserId))
                .orElse(false);
    }

    /** Id of the authenticated user in iam, if it can be resolved. */
    public Optional<Long> findCurrentUserId(Authentication authentication) {
        return Optional.ofNullable(currentUserId(authentication));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        var userId = iamContextFacade.fetchUserIdByUsername(authentication.getName());
        return userId == null || userId == 0L ? null : userId;
    }
}
