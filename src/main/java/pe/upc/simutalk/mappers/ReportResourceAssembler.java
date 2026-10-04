package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.CarbonSavingsReport;
import pe.upc.simutalk.dtos.CarbonSavingsReportResource;
import pe.upc.simutalk.dtos.CompanySummaryReport;
import pe.upc.simutalk.dtos.CompanySummaryReportResource;
import pe.upc.simutalk.dtos.CriterionAveragesReport;
import pe.upc.simutalk.dtos.CriterionAveragesReportResource;
import pe.upc.simutalk.dtos.FunnelReport;
import pe.upc.simutalk.dtos.FunnelReportResource;

public class ReportResourceAssembler {

    public static FunnelReportResource toResource(FunnelReport report) {
        var stages = report.stages().entrySet().stream()
                .map(entry -> new FunnelReportResource.Stage(entry.getKey(), entry.getValue()))
                .toList();
        return new FunnelReportResource(report.jobPostingId(), report.totalApplications(), stages);
    }

    public static CriterionAveragesReportResource toResource(CriterionAveragesReport report) {
        return new CriterionAveragesReportResource(report.jobPostingId(), report.criteria().stream()
                .map(item -> new CriterionAveragesReportResource.Item(item.criterionId(), item.criterionName(),
                        item.averageScore(), item.assessedCandidates()))
                .toList());
    }

    public static CarbonSavingsReportResource toResource(CarbonSavingsReport report) {
        return new CarbonSavingsReportResource(report.jobPostingId(), report.totalKgCo2eAvoided(), report.tripsAvoided(),
                report.totalKmAvoided());
    }

    public static CompanySummaryReportResource toResource(CompanySummaryReport report) {
        return new CompanySummaryReportResource(report.companyId(), report.activeJobPostings(), report.assessedCandidates(),
                report.averageDaysToShortlist(), report.anchoredEvidences());
    }
}
