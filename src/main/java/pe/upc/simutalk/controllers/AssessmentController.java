package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.AssessmentResource;
import pe.upc.simutalk.dtos.CandidateAssessmentResource;
import pe.upc.simutalk.dtos.ComputeAssessmentCommand;
import pe.upc.simutalk.dtos.EvidenceResource;
import pe.upc.simutalk.dtos.GetAssessmentByInterviewSessionIdQuery;
import pe.upc.simutalk.dtos.GetEvidencesByCriterionScoreQuery;
import pe.upc.simutalk.dtos.GetRankingByJobPostingIdQuery;
import pe.upc.simutalk.dtos.RankingResource;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.mappers.AssessmentResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.CandidateAssessmentResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.EvidenceResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.RankingResourceFromEntityAssembler;
import pe.upc.simutalk.securities.AssessmentAccessPolicy;
import pe.upc.simutalk.services.AssessmentCommandService;
import pe.upc.simutalk.services.AssessmentQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Assessments", description = "Puntuación por criterio con evidencia textual y ranking explicable")
public class AssessmentController {

    private final AssessmentCommandService assessmentCommandService;
    private final AssessmentQueryService assessmentQueryService;
    private final AssessmentAccessPolicy assessmentAccess;

    @PostMapping("/interview-sessions/{sessionId}/assessment")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @assessmentAccess.ownsSessionJobPosting(#sessionId, authentication))")
    @Operation(summary = "Calcular la evaluación de una entrevista",
            description = "Solo entrevistas COMPLETED y una vez. Las respuestas se anonimizan antes de ir al proveedor de IA.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evaluación calculada"),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño ni admin"),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada"),
            @ApiResponse(responseCode = "422", description = "Sesión no COMPLETED, ya evaluada o sin evidencia obtenible")
    })
    public ResponseEntity<AssessmentResource> computeAssessment(@PathVariable Long sessionId) {
        var assessment = assessmentCommandService.handle(new ComputeAssessmentCommand(sessionId));
        return ResponseEntity.status(HttpStatus.CREATED).body(AssessmentResourceFromEntityAssembler.toResourceFromEntity(assessment));
    }

    @GetMapping("/interview-sessions/{sessionId}/assessment")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @assessmentAccess.ownsSessionJobPosting(#sessionId, authentication))"
            + " or (hasRole('CANDIDATE') and @assessmentAccess.isSessionCandidate(#sessionId, authentication))")
    @Operation(summary = "Ver la evaluación de una entrevista",
            description = "El recruiter dueño de la vacante (o un admin) ve la evaluación completa. El candidato dueño de "
                    + "la entrevista ve solo su puntaje ponderado, su desglose por criterio y la retroalimentación: nunca "
                    + "su posición en el ranking, los puntajes de otros ni las señales de integridad.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluación (completa o vista del candidato)",
                    content = @Content(schema = @Schema(oneOf = {AssessmentResource.class, CandidateAssessmentResource.class}))),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño, un admin ni el candidato de la entrevista"),
            @ApiResponse(responseCode = "404", description = "La entrevista no tiene evaluación")
    })
    public ResponseEntity<?> getAssessment(@PathVariable Long sessionId, Authentication authentication) {
        var assessment = assessmentQueryService.handle(new GetAssessmentByInterviewSessionIdQuery(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException("Interview session %s has no assessment".formatted(sessionId)));
        if (assessmentAccess.seesFullAssessment(sessionId, authentication)) {
            return ResponseEntity.ok(AssessmentResourceFromEntityAssembler.toResourceFromEntity(assessment));
        }
        return ResponseEntity.ok(CandidateAssessmentResourceFromEntityAssembler.toResourceFromEntity(assessment));
    }

    @GetMapping("/assessments/{assessmentId}/criterion-scores/{criterionScoreId}/evidences")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @assessmentAccess.ownsAssessment(#assessmentId, authentication))")
    @Operation(summary = "Ver la evidencia de un puntaje",
            description = "Fragmentos literales de las respuestas con su posición. Con anonymizedScreening, los datos personales se ocultan.")
    public ResponseEntity<List<EvidenceResource>> getEvidences(@PathVariable Long assessmentId, @PathVariable Long criterionScoreId) {
        var evidences = assessmentQueryService.handle(new GetEvidencesByCriterionScoreQuery(assessmentId, criterionScoreId));
        return ResponseEntity.ok(evidences.stream().map(EvidenceResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    @GetMapping("/job-postings/{jobPostingId}/ranking")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @assessmentAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Ranking explicable de la vacante",
            description = "Candidatos evaluados ordenados por puntaje ponderado, con desglose por criterio e insignias. "
                    + "anonymized=true reemplaza nombre y documento por un código estable; si la vacante tiene "
                    + "anonymizedScreening, la anonimización es forzada.")
    public ResponseEntity<RankingResource> getRanking(
            @PathVariable Long jobPostingId,
            @Parameter(description = "Ocultar nombre y documento") @RequestParam(defaultValue = "false") boolean anonymized) {
        var ranking = assessmentQueryService.handle(new GetRankingByJobPostingIdQuery(jobPostingId, anonymized));
        return ResponseEntity.ok(RankingResourceFromEntityAssembler.toResourceFromEntity(ranking));
    }
}
