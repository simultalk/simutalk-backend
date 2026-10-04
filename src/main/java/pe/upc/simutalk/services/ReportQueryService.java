package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CarbonSavingsReport;
import pe.upc.simutalk.dtos.CompanySummaryReport;
import pe.upc.simutalk.dtos.CriterionAveragesReport;
import pe.upc.simutalk.dtos.FunnelReport;
import pe.upc.simutalk.dtos.GetCarbonSavingsReportQuery;
import pe.upc.simutalk.dtos.GetCompanySummaryReportQuery;
import pe.upc.simutalk.dtos.GetCriterionAveragesReportQuery;
import pe.upc.simutalk.dtos.GetFunnelReportQuery;

public interface ReportQueryService {

    FunnelReport handle(GetFunnelReportQuery query);

    CriterionAveragesReport handle(GetCriterionAveragesReportQuery query);

    CarbonSavingsReport handle(GetCarbonSavingsReportQuery query);

    CompanySummaryReport handle(GetCompanySummaryReportQuery query);
}
