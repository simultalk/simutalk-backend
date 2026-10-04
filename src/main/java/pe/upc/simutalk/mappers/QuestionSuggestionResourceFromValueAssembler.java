package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.QuestionSuggestion;
import pe.upc.simutalk.dtos.QuestionSuggestionResource;
import pe.upc.simutalk.enums.QuestionOrigin;

public class QuestionSuggestionResourceFromValueAssembler {

    public static QuestionSuggestionResource toResourceFromValue(Long criterionId, QuestionSuggestion suggestion) {
        return new QuestionSuggestionResource(criterionId, suggestion.statement(), QuestionOrigin.AI_SUGGESTED,
                suggestion.rationale());
    }
}
