package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.GetQuestionSuggestionsQuery;
import pe.upc.simutalk.dtos.QuestionSuggestion;

import java.util.List;

public interface QuestionSuggestionQueryService {

    /** Proposed questions for a COMPETENCY criterion of a DRAFT job posting; nothing is persisted. */
    List<QuestionSuggestion> handle(GetQuestionSuggestionsQuery query);
}
