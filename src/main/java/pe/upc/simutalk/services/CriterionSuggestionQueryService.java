package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CriterionSuggestion;
import pe.upc.simutalk.dtos.GetCriterionSuggestionsQuery;

import java.util.List;

public interface CriterionSuggestionQueryService {

    /** Proposed criteria for a DRAFT job posting; nothing is persisted and no weight is assigned. */
    List<CriterionSuggestion> handle(GetCriterionSuggestionsQuery query);
}
