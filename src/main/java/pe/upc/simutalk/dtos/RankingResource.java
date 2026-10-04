package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.CriterionKind;
import pe.upc.simutalk.enums.RankingBadge;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param anonymizationForced true when the job posting's anonymizedScreening imposed it
 */
public record RankingResource(Long jobPostingId, boolean anonymized, boolean anonymizationForced,
                              List<RankingEntryResource> candidates) {

    /** candidateId, fullName and documentNumber are null when the ranking is anonymized. */
    public record RankingEntryResource(int rank, String candidateCode, Long candidateId, String fullName,
                                       String documentNumber, Long applicationId, Long assessmentId,
                                       BigDecimal weightedScore, List<CriterionBreakdownResource> criteria,
                                       List<RankingBadge> badges) {
    }

    public record CriterionBreakdownResource(Long criterionScoreId, Long criterionId, String criterionName,
                                             CriterionKind criterionKind, int weightApplied, BigDecimal score,
                                             BigDecimal confidence, int evidenceCount) {
    }
}
