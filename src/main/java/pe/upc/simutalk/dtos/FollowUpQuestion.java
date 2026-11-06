package pe.upc.simutalk.dtos;

/**
 * A follow-up question generated for a regular answer (US-09).
 *
 * @param parentAnswerId the answer it is about; the candidate replies with {@code followUp=true} and this id
 * @param questionId     the script question of that answer, which the reply also uses
 * @param statement      the follow-up question shown to the candidate
 */
public record FollowUpQuestion(Long parentAnswerId, Long questionId, String statement) {
}
