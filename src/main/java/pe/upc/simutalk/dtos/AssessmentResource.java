package pe.upc.simutalk.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AssessmentResource(
        Long id,
        Long interviewSessionId,
        Long applicationId,
        Long jobPostingId,
        BigDecimal weightedScore,
        String engineVersion,
        Instant computedAt,
        List<CriterionScoreResource> criterionScores,
        List<IntegrityFlagResource> integrityFlags,
        String feedbackSummary) {
}
