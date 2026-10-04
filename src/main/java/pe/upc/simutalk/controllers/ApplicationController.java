package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.ApplicationResource;
import pe.upc.simutalk.dtos.GetApplicationsByCandidateIdQuery;
import pe.upc.simutalk.dtos.UpdateApplicationStatusResource;
import pe.upc.simutalk.mappers.ApplicationResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.ChangeApplicationStatusCommandFromResourceAssembler;
import pe.upc.simutalk.securities.RecruitmentAccessPolicy;
import pe.upc.simutalk.services.ApplicationCommandService;
import pe.upc.simutalk.services.ApplicationQueryService;

import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/applications", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Applications", description = "Postulaciones del candidato y cambio de etapa")
public class ApplicationController {

    private final ApplicationCommandService applicationCommandService;
    private final ApplicationQueryService applicationQueryService;
    private final RecruitmentAccessPolicy recruitmentAccessPolicy;

    @GetMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Mis postulaciones", description = "Postulaciones del candidato autenticado, la más reciente primero.")
    public ResponseEntity<List<ApplicationResource>> getMyApplications(Authentication authentication) {
        var applications = recruitmentAccessPolicy.currentCandidateId(authentication)
                .map(candidateId -> applicationQueryService.handle(new GetApplicationsByCandidateIdQuery(candidateId)))
                .orElse(List.of());
        return ResponseEntity.ok(applications.stream()
                .map(ApplicationResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    @PatchMapping("/{applicationId}/status")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @recruitmentAccess.ownsApplicationJobPosting(#applicationId, authentication))")
    @Operation(summary = "Mover una postulación de etapa",
            description = "Solo el recruiter dueño de la vacante (o un admin). Transiciones dirigidas; REJECTED y HIRED son finales.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Etapa actualizada"),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño ni admin"),
            @ApiResponse(responseCode = "404", description = "Postulación no encontrada"),
            @ApiResponse(responseCode = "422", description = "Transición no permitida")
    })
    public ResponseEntity<ApplicationResource> changeStatus(@PathVariable Long applicationId,
                                                            @Valid @RequestBody UpdateApplicationStatusResource resource) {
        var application = applicationCommandService.handle(
                ChangeApplicationStatusCommandFromResourceAssembler.toCommandFromResource(applicationId, resource));
        return ResponseEntity.ok(ApplicationResourceFromEntityAssembler.toResourceFromEntity(application));
    }
}
