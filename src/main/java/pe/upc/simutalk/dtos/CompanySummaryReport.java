package pe.upc.simutalk.dtos;

import java.math.BigDecimal;

/**
 * @param averageDaysToShortlist average days from applying to SHORTLISTED (the "terna"); {@code null} if nobody was shortlisted
 */
public record CompanySummaryReport(Long companyId, long activeJobPostings, long assessedCandidates,
                                   BigDecimal averageDaysToShortlist, long anchoredEvidences) {
}
