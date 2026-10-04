package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CarbonSavingsReport;
import pe.upc.simutalk.dtos.CompanySummaryReport;
import pe.upc.simutalk.dtos.CriterionAveragesReport;
import pe.upc.simutalk.dtos.FunnelReport;
import pe.upc.simutalk.dtos.GetCarbonSavingsReportQuery;
import pe.upc.simutalk.dtos.GetCompanySummaryReportQuery;
import pe.upc.simutalk.dtos.GetCriterionAveragesReportQuery;
import pe.upc.simutalk.dtos.GetFunnelReportQuery;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.repositories.CarbonSavingRepository;
import pe.upc.simutalk.services.ReportQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Reports built only from database aggregations (JPQL COUNT / AVG / SUM with GROUP BY), either
 * analytics' own or exposed by the owning context's facade. No entity list is loaded to count.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportQueryServiceImpl implements ReportQueryService {

    /** Funnel order shown in the dashboard. */
    static final List<String> FUNNEL_STAGES = List.of("RECEIVED", "INTERVIEWING", "ASSESSED", "SHORTLISTED", "HIRED", "REJECTED");
    private static final BigDecimal SECONDS_PER_DAY = BigDecimal.valueOf(86_400);

    private final AnalyticsExternalContextsService externalContextsService;
    private final CarbonSavingRepository carbonSavingRepository;

    @Override
    public FunnelReport handle(GetFunnelReportQuery query) {
        ensureJobPostingExists(query.jobPostingId());
        var counts = externalContextsService.countApplicationsByStatus(query.jobPostingId());
        var stages = new LinkedHashMap<String, Long>();
        FUNNEL_STAGES.forEach(stage -> stages.put(stage, counts.getOrDefault(stage, 0L)));
        var total = stages.values().stream().mapToLong(Long::longValue).sum();
        return new FunnelReport(query.jobPostingId(), stages, total);
    }

    @Override
    public CriterionAveragesReport handle(GetCriterionAveragesReportQuery query) {
        ensureJobPostingExists(query.jobPostingId());
        var items = externalContextsService.fetchCriterionAverages(query.jobPostingId()).stream()
                .map(average -> new CriterionAveragesReport.Item(average.criterionId(), average.criterionName(),
                        average.averageScore(), average.assessedCount()))
                .toList();
        return new CriterionAveragesReport(query.jobPostingId(), items);
    }

    @Override
    public CarbonSavingsReport handle(GetCarbonSavingsReportQuery query) {
        ensureJobPostingExists(query.jobPostingId());
        var totals = carbonSavingRepository.totalsByJobPostingId(query.jobPostingId());
        return new CarbonSavingsReport(query.jobPostingId(), totals.getTotalKg().setScale(3, RoundingMode.HALF_UP),
                totals.getTrips(), totals.getTotalKm().setScale(2, RoundingMode.HALF_UP));
    }

    @Override
    public CompanySummaryReport handle(GetCompanySummaryReportQuery query) {
        var jobPostingIds = externalContextsService.fetchJobPostingIds(query.companyId());
        var averageSeconds = externalContextsService.fetchAverageSecondsToShortlist(jobPostingIds);
        return new CompanySummaryReport(query.companyId(),
                externalContextsService.countPublishedJobPostings(query.companyId()),
                externalContextsService.countAssessments(jobPostingIds),
                averageSeconds == null ? null : toDays(averageSeconds),
                externalContextsService.countEvidences(jobPostingIds));
    }

    static BigDecimal toDays(long seconds) {
        return BigDecimal.valueOf(seconds).divide(SECONDS_PER_DAY, 1, RoundingMode.HALF_UP);
    }

    private void ensureJobPostingExists(Long jobPostingId) {
        if (!externalContextsService.existsJobPosting(jobPostingId)) {
            throw new ResourceNotFoundException("Job posting", jobPostingId);
        }
    }
}
