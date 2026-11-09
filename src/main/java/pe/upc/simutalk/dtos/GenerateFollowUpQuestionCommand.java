package pe.upc.simutalk.dtos;

/**
 * US-09: asks for the follow-up question of a regular answer of an interview session.
 *
 * @param answerId the regular answer the follow-up question is about
 */
public record GenerateFollowUpQuestionCommand(Long interviewSessionId, Long answerId) {
}
