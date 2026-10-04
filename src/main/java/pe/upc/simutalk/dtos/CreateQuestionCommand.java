package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.QuestionOrigin;

/** The new question is appended at the end of the script. */
public record CreateQuestionCommand(Long jobPostingId, Long criterionId, String statement, int maxDurationSeconds,
                                    QuestionOrigin origin, boolean allowsFollowUp) {
}
