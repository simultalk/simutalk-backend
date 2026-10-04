package pe.upc.simutalk.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * What the candidate sees of their own assessment: the weighted score, the breakdown by criterion and the
 * feedback. Never the ranking position, other candidates' scores or the integrity flags raised on them.
 */
@Schema(description = "Vista del candidato: su puntaje, su desglose por criterio y la retroalimentación. "
        + "Sin posición en el ranking, sin puntajes de otros y sin señales de integridad.")
public record CandidateAssessmentResource(
        Long interviewSessionId,
        Long jobPostingId,
        BigDecimal weightedScore,
        Instant computedAt,
        List<CandidateCriterionScoreResource> criterionScores,
        String feedbackSummary) {
}
