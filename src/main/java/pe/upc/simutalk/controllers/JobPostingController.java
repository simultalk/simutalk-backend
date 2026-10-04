package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.CreateJobPostingResource;
import pe.upc.simutalk.dtos.DeleteJobPostingCommand;
import pe.upc.simutalk.dtos.GetJobPostingByIdQuery;
import pe.upc.simutalk.dtos.JobPostingResource;
import pe.upc.simutalk.dtos.SearchJobPostingsQuery;
import pe.upc.simutalk.dtos.UpdateJobPostingResource;
import pe.upc.simutalk.dtos.UpdateJobPostingStatusResource;
import pe.upc.simutalk.enums.JobPostingStatus;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.mappers.ChangeJobPostingStatusCommandFromResourceAssembler;
import pe.upc.simutalk.mappers.CreateJobPostingCommandFromResourceAssembler;
import pe.upc.simutalk.mappers.JobPostingResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.UpdateJobPostingCommandFromResourceAssembler;
import pe.upc.simutalk.securities.RecruitmentAccessPolicy;
import pe.upc.simutalk.services.JobPostingCommandService;
import pe.upc.simutalk.services.JobPostingQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/job-postings", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Job Postings", description = "Vacantes y su ciclo de vida")
public class JobPostingController {

    private final JobPostingCommandService jobPostingCommandService;
    private final JobPostingQueryService jobPostingQueryService;
    private final RecruitmentAccessPolicy recruitmentAccessPolicy;

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Crear una vacante",
            description = "Se crea en DRAFT, sin criterios, a nombre de la empresa del usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vacante creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No es recruiter ni admin"),
            @ApiResponse(responseCode = "422", description = "El usuario no tiene perfil de empresa")
    })
    public ResponseEntity<JobPostingResource> createJobPosting(@Valid @RequestBody CreateJobPostingResource resource,
                                                               Authentication authentication) {
        var companyId = recruitmentAccessPolicy.currentCompanyId(authentication)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "The authenticated user has no company profile to publish job postings for"));
        var command = CreateJobPostingCommandFromResourceAssembler.toCommandFromResource(resource, companyId);
        var jobPosting = jobPostingCommandService.handle(command);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(jobPosting.getId()).toUri();
        return ResponseEntity.created(location).body(JobPostingResourceFromEntityAssembler.toResourceFromEntity(jobPosting));
    }

    @GetMapping("/{jobPostingId}")
    @Operation(summary = "Obtener una vacante con sus criterios",
            description = "Un DRAFT solo lo ve su propia empresa; para las demás responde 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vacante encontrada"),
            @ApiResponse(responseCode = "404", description = "Vacante no encontrada")
    })
    public ResponseEntity<JobPostingResource> getJobPostingById(@PathVariable Long jobPostingId,
                                                                Authentication authentication) {
        var viewer = recruitmentAccessPolicy.currentViewer(authentication);
        var jobPosting = jobPostingQueryService.handle(new GetJobPostingByIdQuery(jobPostingId, viewer))
                .orElseThrow(() -> new ResourceNotFoundException("Job posting", jobPostingId));
        return ResponseEntity.ok(JobPostingResourceFromEntityAssembler.toResourceFromEntity(jobPosting));
    }

    @GetMapping
    @Operation(summary = "Listar vacantes",
            description = "Filtros opcionales y combinables. De otras empresas solo se listan las PUBLISHED.")
    public ResponseEntity<List<JobPostingResource>> getJobPostings(
            @Parameter(description = "Filtra por empresa") @RequestParam(required = false) Long companyId,
            @Parameter(description = "Filtra por estado") @RequestParam(required = false) JobPostingStatus status,
            Authentication authentication) {
        var viewer = recruitmentAccessPolicy.currentViewer(authentication);
        var jobPostings = jobPostingQueryService.handle(new SearchJobPostingsQuery(companyId, status, viewer));
        return ResponseEntity.ok(jobPostings.stream()
                .map(JobPostingResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    @PutMapping("/{jobPostingId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @recruitmentAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Actualizar los datos de una vacante",
            description = "No se permite sobre vacantes CLOSED. anonymizedScreening solo cambia en DRAFT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vacante actualizada"),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño ni admin"),
            @ApiResponse(responseCode = "404", description = "Vacante no encontrada"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    public ResponseEntity<JobPostingResource> updateJobPosting(@PathVariable Long jobPostingId,
                                                               @Valid @RequestBody UpdateJobPostingResource resource) {
        var command = UpdateJobPostingCommandFromResourceAssembler.toCommandFromResource(jobPostingId, resource);
        var jobPosting = jobPostingCommandService.handle(command);
        return ResponseEntity.ok(JobPostingResourceFromEntityAssembler.toResourceFromEntity(jobPosting));
    }

    @DeleteMapping("/{jobPostingId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @recruitmentAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Eliminar una vacante", description = "Una vacante PUBLISHED debe cerrarse antes de eliminarse.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vacante eliminada"),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño ni admin"),
            @ApiResponse(responseCode = "404", description = "Vacante no encontrada"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    public ResponseEntity<Void> deleteJobPosting(@PathVariable Long jobPostingId) {
        jobPostingCommandService.handle(new DeleteJobPostingCommand(jobPostingId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{jobPostingId}/status")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @recruitmentAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Cambiar el estado de una vacante",
            description = "DRAFT → PUBLISHED exige al menos un criterio y pesos que sumen exactamente 100. "
                    + "DRAFT/PUBLISHED → CLOSED. No se vuelve a DRAFT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño ni admin"),
            @ApiResponse(responseCode = "404", description = "Vacante no encontrada"),
            @ApiResponse(responseCode = "422", description = "Transición no permitida")
    })
    public ResponseEntity<JobPostingResource> changeJobPostingStatus(@PathVariable Long jobPostingId,
                                                                     @Valid @RequestBody UpdateJobPostingStatusResource resource) {
        var command = ChangeJobPostingStatusCommandFromResourceAssembler.toCommandFromResource(jobPostingId, resource);
        var jobPosting = jobPostingCommandService.handle(command);
        return ResponseEntity.ok(JobPostingResourceFromEntityAssembler.toResourceFromEntity(jobPosting));
    }
}
