package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.CarbonSavingsReportResource;
import pe.upc.simutalk.dtos.CompanySummaryReportResource;
import pe.upc.simutalk.dtos.CriterionAveragesReportResource;
import pe.upc.simutalk.dtos.FunnelReportResource;
import pe.upc.simutalk.dtos.GetCarbonSavingsReportQuery;
import pe.upc.simutalk.dtos.GetCompanySummaryReportQuery;
import pe.upc.simutalk.dtos.GetCriterionAveragesReportQuery;
import pe.upc.simutalk.dtos.GetFunnelReportQuery;
import pe.upc.simutalk.mappers.ReportResourceAssembler;
import pe.upc.simutalk.services.ReportQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/reports", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports", description = "Reportes de la vacante y de la empresa")
public class ReportController {

    private final ReportQueryService reportQueryService;

    @GetMapping("/job-postings/{jobPostingId}/funnel")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @analyticsAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Embudo de la vacante", description = "Postulaciones por etapa: RECEIVED, INTERVIEWING, ASSESSED, SHORTLISTED, HIRED, REJECTED.")
    public ResponseEntity<FunnelReportResource> getFunnel(@PathVariable Long jobPostingId) {
        return ResponseEntity.ok(ReportResourceAssembler.toResource(reportQueryService.handle(new GetFunnelReportQuery(jobPostingId))));
    }

    @GetMapping("/job-postings/{jobPostingId}/criterion-averages")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @analyticsAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Puntaje medio por criterio", description = "Sobre los candidatos evaluados de la vacante.")
    public ResponseEntity<CriterionAveragesReportResource> getCriterionAverages(@PathVariable Long jobPostingId) {
        return ResponseEntity.ok(ReportResourceAssembler.toResource(
                reportQueryService.handle(new GetCriterionAveragesReportQuery(jobPostingId))));
    }

    @GetMapping("/job-postings/{jobPostingId}/carbon-savings")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @analyticsAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Emisiones evitadas", description = "kg CO2e evitados y traslados no realizados gracias a la entrevista asincrónica.")
    public ResponseEntity<CarbonSavingsReportResource> getCarbonSavings(@PathVariable Long jobPostingId) {
        return ResponseEntity.ok(ReportResourceAssembler.toResource(
                reportQueryService.handle(new GetCarbonSavingsReportQuery(jobPostingId))));
    }

    @GetMapping("/companies/{companyId}/summary")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @analyticsAccess.isCompany(#companyId, authentication))")
    @Operation(summary = "Resumen de la empresa",
            description = "Vacantes activas, candidatos evaluados, días promedio hasta la terna (SHORTLISTED) y evidencias ancladas.")
    public ResponseEntity<CompanySummaryReportResource> getCompanySummary(@PathVariable Long companyId) {
        return ResponseEntity.ok(ReportResourceAssembler.toResource(
                reportQueryService.handle(new GetCompanySummaryReportQuery(companyId))));
    }
}
