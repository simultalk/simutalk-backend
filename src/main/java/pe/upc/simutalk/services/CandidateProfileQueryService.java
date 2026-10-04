package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.GetAllCandidateProfilesQuery;
import pe.upc.simutalk.dtos.GetCandidateProfileByIdQuery;
import pe.upc.simutalk.dtos.GetCandidateProfileByUserIdQuery;
import pe.upc.simutalk.dtos.GetCertificationsByCandidateIdQuery;
import pe.upc.simutalk.dtos.GetVerifiedCertificationCountQuery;
import pe.upc.simutalk.entities.CandidateProfile;
import pe.upc.simutalk.entities.Certification;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface CandidateProfileQueryService {

    Optional<CandidateProfile> handle(GetCandidateProfileByIdQuery query);

    /** Page of candidates ordered by id, certifications loaded. */
    Page<CandidateProfile> handle(GetAllCandidateProfilesQuery query);

    Optional<CandidateProfile> handle(GetCandidateProfileByUserIdQuery query);

    /**
     * @throws ResourceNotFoundException if the candidate does not exist
     */
    List<Certification> handle(GetCertificationsByCandidateIdQuery query);

    /**
     * @return certifications that are VERIFIED and not expired today; 0 if the candidate does not exist
     */
    long handle(GetVerifiedCertificationCountQuery query);
}
