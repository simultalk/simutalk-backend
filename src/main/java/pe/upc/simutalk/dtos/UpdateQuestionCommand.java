package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.QuestionOrigin;

/** The position changes only through ReorderQuestionsCommand. */
public record UpdateQuestionCommand(Long jobPostingId, Long questionId, Long criterionId, String statement,
                                    int maxDurationSeconds, QuestionOrigin origin, boolean allowsFollowUp) {
}
