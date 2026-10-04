package pe.upc.simutalk.dtos;

import java.util.List;

/**
 * @param anonymized       whether names and documents were replaced by candidate codes
 * @param anonymizationForced whether the job posting's anonymizedScreening imposed it
 */
public record Ranking(Long jobPostingId, boolean anonymized, boolean anonymizationForced, List<RankingEntry> entries) {
}
