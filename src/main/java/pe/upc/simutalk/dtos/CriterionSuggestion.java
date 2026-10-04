package pe.upc.simutalk.dtos;

/**
 * A criterion proposed from the job description. It is never persisted and, on purpose, has no
 * weight: the system never assigns a weight on its own, the recruiter always does when accepting it.
 *
 * @param name        proposed criterion name
 * @param description what the criterion evaluates
 * @param rationale   why it was proposed (e.g. the keywords of the description it comes from)
 */
public record CriterionSuggestion(String name, String description, String rationale) {

    public CriterionSuggestion {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Criterion suggestion name is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Criterion suggestion description is required");
        }
        name = name.strip();
        description = description.strip();
        rationale = rationale == null ? "" : rationale.strip();
    }
}
