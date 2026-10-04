package pe.upc.simutalk.dtos;

import java.math.BigDecimal;

/**
 * @param tripsAvoided round trips to the company that did not happen (one per asynchronous interview)
 */
public record CarbonSavingsReport(Long jobPostingId, BigDecimal totalKgCo2eAvoided, long tripsAvoided,
                                  BigDecimal totalKmAvoided) {
}
