package pe.upc.simutalk.dtos;

/**
 * @param anonymizedRequested the caller asked for an anonymized ranking; the job posting may force it anyway
 */
public record GetRankingByJobPostingIdQuery(Long jobPostingId, boolean anonymizedRequested) {
}
