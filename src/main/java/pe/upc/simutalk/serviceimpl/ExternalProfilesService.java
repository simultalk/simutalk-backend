package pe.upc.simutalk.serviceimpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.upc.simutalk.dtos.CandidatePersonalData;
import pe.upc.simutalk.services.ProfilesContextFacade;

/**
 * Anti-corruption layer from interviews to profiles, through the ProfilesContextFacade contract in shared.
 */
@Service
@RequiredArgsConstructor
public class ExternalProfilesService {

    private final ProfilesContextFacade profilesContextFacade;

    /** PII: only to redact the answer before it reaches the AI provider; may be {@code null}. */
    public CandidatePersonalData fetchCandidatePersonalData(Long candidateId) {
        return profilesContextFacade.fetchCandidatePersonalData(candidateId);
    }
}
