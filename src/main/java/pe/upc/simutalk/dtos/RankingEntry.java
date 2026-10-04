package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.CriterionKind;
import pe.upc.simutalk.enums.RankingBadge;

import java.math.BigDecimal;
import java.util.List;

/**
 * One row of the explainable ranking.
 *
 * @param candidateId    {@code null} when the ranking is anonymized
 * @param fullName       {@code null} when the ranking is anonymized
 * @param documentNumber {@code null} when the ranking is anonymized
 */
public record RankingEntry(int rank, Long assessmentId, Long applicationId, Long candidateId, String candidateCode,
                           String fullName, String documentNumber, BigDecimal weightedScore,
                           List<CriterionBreakdown> criteria, List<RankingBadge> badges) {

    public record CriterionBreakdown(Long criterionScoreId, Long criterionId, String criterionName, CriterionKind kind,
                                     int weightApplied, BigDecimal score, BigDecimal confidence, int evidenceCount) {
    }
}
