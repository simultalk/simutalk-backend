package pe.upc.simutalk.dtos;

/**
 * @param criterionId the COMPETENCY criterion of the job posting the questions are proposed for
 */
public record GetQuestionSuggestionsQuery(Long jobPostingId, Long criterionId) {
}
