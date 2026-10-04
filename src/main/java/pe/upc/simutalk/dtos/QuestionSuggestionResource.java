package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.QuestionOrigin;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A proposed question. To accept it, the recruiter sends it to
 * {@code POST /api/v1/job-postings/{id}/questions} with {@code origin=AI_SUGGESTED}, the answer time
 * ({@code maxDurationSeconds}) and whether it allows a follow-up.
 */
@Schema(description = "Pregunta propuesta, sin persistir: el reclutador la acepta con POST /questions y origin=AI_SUGGESTED")
public record QuestionSuggestionResource(
        @Schema(example = "1", description = "Criterio COMPETENCY para el que se propuso") Long criterionId,
        @Schema(example = "Cuéntame una situación concreta en la que hayas puesto en práctica la competencia «Pensamiento analítico». ¿Qué hiciste y cuál fue el resultado?")
        String statement,
        @Schema(example = "AI_SUGGESTED") QuestionOrigin origin,
        @Schema(example = "Pregunta conductual: una experiencia real muestra cómo aplicó la competencia") String rationale) {
}
