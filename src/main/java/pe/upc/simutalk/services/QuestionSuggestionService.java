package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.QuestionSuggestion;

import java.util.List;

/**
 * Port to the AI provider that proposes interview questions for one COMPETENCY criterion of a job
 * posting. Adapters live in infrastructure/external/ai.
 * <p>
 * Only the job posting's and the criterion's own text travel to the provider, plus the questions the
 * recruiter already wrote: the port takes no candidate data at all, and none exists while the posting
 * is in DRAFT.
 */
public interface QuestionSuggestionService {

    /**
     * @param jobTitle             job posting title
     * @param jobDescription       job posting description
     * @param criterionName        name of the COMPETENCY criterion the questions must evaluate
     * @param criterionDescription what the criterion evaluates
     * @param existingQuestions    statements already in the job posting's script, which must not be proposed again
     * @return proposed questions for the criterion; empty if none could be derived
     */
    List<QuestionSuggestion> suggest(String jobTitle, String jobDescription, String criterionName,
                                     String criterionDescription, List<String> existingQuestions);
}
