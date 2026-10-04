package pe.upc.simutalk.dtos;

import java.math.BigDecimal;

/**
 * @param averageDaysToShortlist null when no application has been shortlisted yet
 */
public record CompanySummaryReportResource(Long companyId, long activeJobPostings, long assessedCandidates,
                                           BigDecimal averageDaysToShortlist, long anchoredEvidences) {
}
