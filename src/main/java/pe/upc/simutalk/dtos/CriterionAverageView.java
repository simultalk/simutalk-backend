package pe.upc.simutalk.dtos;

import java.math.BigDecimal;

/**
 * Average score of one criterion over the assessed candidates of a job posting.
 *
 * @param assessedCount number of assessments that scored the criterion
 */
public record CriterionAverageView(Long criterionId, String criterionName, BigDecimal averageScore, long assessedCount) {
}
