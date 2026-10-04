package pe.upc.simutalk.dtos;

import pe.upc.simutalk.entities.Question;

/**
 * An interview question proposed for a COMPETENCY criterion. It is never persisted: to accept it, the
 * recruiter adds it to the script like any other question, with {@code origin=AI_SUGGESTED}, choosing
 * the answer time and whether it allows a follow-up.
 * <p>
 * A suggestion always fits in a script question: the statement is not blank and has at most
 * {@value Question#STATEMENT_MAX_LENGTH} characters.
 *
 * @param statement proposed question
 * @param rationale what the answer to the question lets the recruiter observe
 */
public record QuestionSuggestion(String statement, String rationale) {

    public QuestionSuggestion {
        if (statement == null || statement.isBlank()) {
            throw new IllegalArgumentException("Question suggestion statement is required");
        }
        statement = statement.strip();
        if (statement.length() > Question.STATEMENT_MAX_LENGTH) {
            throw new IllegalArgumentException("Question suggestion statement must be at most %d characters"
                    .formatted(Question.STATEMENT_MAX_LENGTH));
        }
        rationale = rationale == null ? "" : rationale.strip();
    }
}
