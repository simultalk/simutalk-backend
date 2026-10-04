package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.CriterionOrigin;
import pe.upc.simutalk.enums.CriterionType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A proposed criterion. It has no weight on purpose: to accept it, the recruiter sends it to
 * {@code POST /api/v1/job-postings/{id}/criteria} with the weight they choose and {@code origin=AI_SUGGESTED}.
 */
@Schema(description = "Criterio propuesto, sin persistir y sin peso: el peso lo asigna siempre el reclutador")
public record CriterionSuggestionResource(
        @Schema(example = "Pensamiento analítico") String name,
        @Schema(example = "Descompone problemas y los resuelve apoyándose en datos.") String description,
        @Schema(example = "COMPETENCY") CriterionType criterionType,
        @Schema(example = "AI_SUGGESTED") CriterionOrigin origin,
        @Schema(example = "La descripción menciona: datos, sql, excel") String rationale) {
}
