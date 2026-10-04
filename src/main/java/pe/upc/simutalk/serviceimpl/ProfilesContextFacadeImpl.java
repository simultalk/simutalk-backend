package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CandidateContact;
import pe.upc.simutalk.dtos.CandidatePersonalData;
import pe.upc.simutalk.dtos.GetCandidateProfileByIdQuery;
import pe.upc.simutalk.dtos.GetCandidateProfileByUserIdQuery;
import pe.upc.simutalk.dtos.GetCompanyProfileByIdQuery;
import pe.upc.simutalk.dtos.GetCompanyProfileByUserIdQuery;
import pe.upc.simutalk.dtos.GetVerifiedCertificationCountQuery;
import pe.upc.simutalk.entities.CandidateProfile;
import pe.upc.simutalk.entities.CompanyProfile;
import pe.upc.simutalk.services.CandidateProfileQueryService;
import pe.upc.simutalk.services.CompanyProfileQueryService;
import pe.upc.simutalk.services.ProfilesContextFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * profiles' implementation of the {@link ProfilesContextFacade} contract published in shared.
 */
@Service
@RequiredArgsConstructor
public class ProfilesContextFacadeImpl implements ProfilesContextFacade {

    private final CandidateProfileQueryService candidateProfileQueryService;
    private final CompanyProfileQueryService companyProfileQueryService;

    @Override
    public Long fetchCandidateIdByUserId(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return candidateProfileQueryService.handle(new GetCandidateProfileByUserIdQuery(userId))
                .map(CandidateProfile::getId)
                .orElse(0L);
    }

    @Override
    public Long fetchCompanyIdByUserId(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return companyProfileQueryService.handle(new GetCompanyProfileByUserIdQuery(userId))
                .map(CompanyProfile::getId)
                .orElse(0L);
    }

    @Override
    public String fetchCompanyDistrict(Long companyId) {
        if (companyId == null) {
            return "";
        }
        return companyProfileQueryService.handle(new GetCompanyProfileByIdQuery(companyId))
                .map(CompanyProfile::getDistrict)
                .orElse("");
    }

    @Override
    public long fetchVerifiedCertificationCount(Long candidateId) {
        if (candidateId == null) {
            return 0L;
        }
        return candidateProfileQueryService.handle(new GetVerifiedCertificationCountQuery(candidateId));
    }

    @Override
    public long fetchRejectedCertificationCount(Long candidateId) {
        if (candidateId == null) {
            return 0L;
        }
        return candidateProfileQueryService.handle(new GetCandidateProfileByIdQuery(candidateId))
                .map(CandidateProfile::countRejectedCertifications)
                .orElse(0L);
    }

    @Override
    public CandidatePersonalData fetchCandidatePersonalData(Long candidateId) {
        if (candidateId == null) {
            return null;
        }
        return candidateProfileQueryService.handle(new GetCandidateProfileByIdQuery(candidateId))
                .map(candidate -> new CandidatePersonalData(candidate.getId(), candidate.getPersonName().firstName(),
                        candidate.getPersonName().lastName(), candidate.getDocumentNumber().value(),
                        candidate.getPhone(), candidate.getDistrict(), candidate.getBirthDate(),
                        candidate.getEmail() == null ? null : candidate.getEmail().value()))
                .orElse(null);
    }

    @Override
    public CandidateContact fetchCandidateContact(Long candidateId) {
        if (candidateId == null) {
            return null;
        }
        return candidateProfileQueryService.handle(new GetCandidateProfileByIdQuery(candidateId))
                .map(candidate -> new CandidateContact(candidate.getId(), candidate.getPersonName().firstName(),
                        candidate.getPersonName().firstName() + " " + candidate.getPersonName().lastName(),
                        candidate.getEmail() == null ? null : candidate.getEmail().value()))
                .orElse(null);
    }

    @Override
    public String fetchCompanyEmail(Long companyId) {
        if (companyId == null) {
            return "";
        }
        return companyProfileQueryService.handle(new GetCompanyProfileByIdQuery(companyId))
                .map(company -> company.getEmail() == null ? "" : company.getEmail().value())
                .orElse("");
    }

    @Override
    public String fetchCandidateDistrict(Long candidateId) {
        if (candidateId == null) {
            return "";
        }
        return candidateProfileQueryService.handle(new GetCandidateProfileByIdQuery(candidateId))
                .map(CandidateProfile::getDistrict)
                .orElse("");
    }
}
