package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CriterionSuggestion;

import java.util.List;

/**
 * Port to the AI provider that proposes evaluation criteria from a job description. Adapters live in
 * infrastructure/external/ai. The result never carries weights: those are always the recruiter's.
 * <p>
 * Only the job posting's own text travels to the provider: no candidate data exists at this point.
 */
public interface CriterionSuggestionService {

    /**
     * @param title             job posting title
     * @param description       job posting description
     * @param existingCriteria  names already defined in the posting, which must not be proposed again
     * @return proposed criteria, without weights; empty if none could be derived
     */
    List<CriterionSuggestion> suggest(String title, String description, List<String> existingCriteria);
}
