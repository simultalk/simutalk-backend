package pe.upc.simutalk.dtos;

import java.math.BigDecimal;
import java.util.List;

public record CriterionAveragesReport(Long jobPostingId, List<Item> criteria) {

    public record Item(Long criterionId, String criterionName, BigDecimal averageScore, long assessedCandidates) {
    }
}
