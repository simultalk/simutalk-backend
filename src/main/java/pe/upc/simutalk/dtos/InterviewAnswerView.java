package pe.upc.simutalk.dtos;

/**
 * Read-only view of an interview answer, shared through {@link InterviewsContextFacade}.
 *
 * @param criterionId criterion evaluated by the question that was answered
 */
public record InterviewAnswerView(Long answerId, Long questionId, Long criterionId, String transcript, boolean followUp) {
}
