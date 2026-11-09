package pe.upc.simutalk.services;

/**
 * Port to the AI provider that writes the follow-up question of an interview answer (US-09). Adapters
 * live in infrastructure/external/ai.
 * <p>
 * Privacy (Ley 29733): callers pass the ALREADY ANONYMIZED answer; besides it only the question's and the
 * criterion's own text reach the provider.
 */
public interface FollowUpQuestionService {

    /**
     * @param questionStatement    the script question that was answered
     * @param criterionName        the COMPETENCY criterion that question evaluates
     * @param criterionDescription what the criterion evaluates
     * @param anonymizedAnswer     the candidate's answer, already anonymized
     * @return the follow-up question, never blank and at most {@code Question.STATEMENT_MAX_LENGTH} characters
     */
    String generate(String questionStatement, String criterionName, String criterionDescription, String anonymizedAnswer);
}
