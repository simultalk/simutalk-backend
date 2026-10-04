package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.CriterionSuggestion;
import pe.upc.simutalk.dtos.CriterionSuggestionResource;
import pe.upc.simutalk.enums.CriterionOrigin;
import pe.upc.simutalk.enums.CriterionType;

public class CriterionSuggestionResourceFromValueAssembler {

    public static CriterionSuggestionResource toResourceFromValue(CriterionSuggestion suggestion) {
        return new CriterionSuggestionResource(suggestion.name(), suggestion.description(), CriterionType.COMPETENCY,
                CriterionOrigin.AI_SUGGESTED, suggestion.rationale());
    }
}
