package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.CreateEvaluationCriterionResource;
import pe.upc.simutalk.dtos.CriterionSuggestionResource;
import pe.upc.simutalk.dtos.EvaluationCriterionResource;
import pe.upc.simutalk.dtos.GetCriterionSuggestionsQuery;
import pe.upc.simutalk.dtos.RemoveEvaluationCriterionCommand;
import pe.upc.simutalk.dtos.UpdateEvaluationCriterionResource;
import pe.upc.simutalk.mappers.AddEvaluationCriterionCommandFromResourceAssembler;
import pe.upc.simutalk.mappers.CriterionSuggestionResourceFromValueAssembler;
import pe.upc.simutalk.mappers.EvaluationCriterionResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.UpdateEvaluationCriterionCommandFromResourceAssembler;
import pe.upc.simutalk.services.CriterionSuggestionQueryService;
import pe.upc.simutalk.services.JobPostingCommandService;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/job-postings/{jobPostingId}/criteria", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Job Posting Criteria", description = "Criterios de evaluación ponderados de una vacante")
public class EvaluationCriterionController {

    private final JobPostingCommandService jobPostingCommandService;
    private final CriterionSuggestionQueryService criterionSuggestionQueryService;

    @PostMapping("/suggestions")
    @PreAuthorize("hasRole('RECRUITER') and @recruitmentAccess.ownsJobPosting(#jobPostingId, authentication)")
    @Operation(summary = "Sugerir criterios a partir de la descripción del puesto",
            description = "Solo el recruiter dueño y con la vacante en DRAFT. Devuelve criterios propuestos SIN "
                    + "persistirlos y SIN peso: el reclutador acepta los que quiera con POST /criteria, poniendo él "
                    + "el peso y origin=AI_SUGGESTED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Criterios propuestos"),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño de la vacante"),
            @ApiResponse(responseCode = "404", description = "Vacante no encontrada"),
            @ApiResponse(responseCode = "422", description = "La vacante no está en DRAFT")
    })
    public ResponseEntity<List<CriterionSuggestionResource>> suggestCriteria(@PathVariable Long jobPostingId) {
        var suggestions = criterionSuggestionQueryService.handle(new GetCriterionSuggestionsQuery(jobPostingId));
        return ResponseEntity.ok(suggestions.stream()
                .map(CriterionSuggestionResourceFromValueAssembler::toResourceFromValue)
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @recruitmentAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Agregar un criterio de evaluación", description = "Solo mientras la vacante está en DRAFT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criterio agregado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño ni admin"),
            @ApiResponse(responseCode = "404", description = "Vacante no encontrada"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    public ResponseEntity<EvaluationCriterionResource> addCriterion(@PathVariable Long jobPostingId,
                                                                    @Valid @RequestBody CreateEvaluationCriterionResource resource) {
        var command = AddEvaluationCriterionCommandFromResourceAssembler.toCommandFromResource(jobPostingId, resource);
        var criterion = jobPostingCommandService.handle(command);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{criterionId}").buildAndExpand(criterion.getId()).toUri();
        return ResponseEntity.created(location)
                .body(EvaluationCriterionResourceFromEntityAssembler.toResourceFromEntity(criterion));
    }

    @PutMapping("/{criterionId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @recruitmentAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Actualizar un criterio de evaluación", description = "Solo mientras la vacante está en DRAFT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Criterio actualizado"),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño ni admin"),
            @ApiResponse(responseCode = "404", description = "Vacante o criterio no encontrado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    public ResponseEntity<EvaluationCriterionResource> updateCriterion(@PathVariable Long jobPostingId,
                                                                       @PathVariable Long criterionId,
                                                                       @Valid @RequestBody UpdateEvaluationCriterionResource resource) {
        var command = UpdateEvaluationCriterionCommandFromResourceAssembler
                .toCommandFromResource(jobPostingId, criterionId, resource);
        var criterion = jobPostingCommandService.handle(command);
        return ResponseEntity.ok(EvaluationCriterionResourceFromEntityAssembler.toResourceFromEntity(criterion));
    }

    @DeleteMapping("/{criterionId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @recruitmentAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Eliminar un criterio de evaluación", description = "Solo mientras la vacante está en DRAFT.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Criterio eliminado"),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño ni admin"),
            @ApiResponse(responseCode = "404", description = "Vacante o criterio no encontrado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    public ResponseEntity<Void> removeCriterion(@PathVariable Long jobPostingId, @PathVariable Long criterionId) {
        jobPostingCommandService.handle(new RemoveEvaluationCriterionCommand(jobPostingId, criterionId));
        return ResponseEntity.noContent().build();
    }
}
