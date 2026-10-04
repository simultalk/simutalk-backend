package pe.upc.simutalk.dtos;

import java.math.BigDecimal;

public record CarbonSavingsReportResource(Long jobPostingId, BigDecimal totalKgCo2eAvoided, long tripsAvoided,
                                          BigDecimal totalKmAvoided) {
}
