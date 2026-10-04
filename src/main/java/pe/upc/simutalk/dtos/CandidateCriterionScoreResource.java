package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.CriterionKind;

import java.math.BigDecimal;

public record CandidateCriterionScoreResource(
        String criterionName,
        CriterionKind criterionKind,
        int weightApplied,
        BigDecimal score) {
}
