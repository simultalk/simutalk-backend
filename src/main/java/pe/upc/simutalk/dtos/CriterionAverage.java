package pe.upc.simutalk.dtos;

import java.math.BigDecimal;

public record CriterionAverage(Long criterionId, String criterionName, BigDecimal averageScore, long assessedCount) {
}
