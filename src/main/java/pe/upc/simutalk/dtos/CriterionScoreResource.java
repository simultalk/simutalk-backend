package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.CriterionKind;

import java.math.BigDecimal;

public record CriterionScoreResource(
        Long id,
        Long criterionId,
        String criterionName,
        CriterionKind criterionKind,
        BigDecimal score,
        int weightApplied,
        BigDecimal confidence,
        int evidenceCount) {
}
