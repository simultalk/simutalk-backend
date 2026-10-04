package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.AnswerResource;
import pe.upc.simutalk.dtos.CompleteInterviewSessionCommand;
import pe.upc.simutalk.dtos.CreateInterviewSessionResource;
import pe.upc.simutalk.dtos.GetAnswersByInterviewSessionIdQuery;
import pe.upc.simutalk.dtos.GetInterviewSessionByApplicationIdQuery;
import pe.upc.simutalk.dtos.InterviewSessionResource;
import pe.upc.simutalk.dtos.RecordAnswerResource;
import pe.upc.simutalk.dtos.StartInterviewSessionCommand;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.mappers.AnswerResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.CreateInterviewSessionCommandFromResourceAssembler;
import pe.upc.simutalk.mappers.InterviewSessionResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.RecordAnswerCommandFromResourceAssembler;
import pe.upc.simutalk.services.InterviewSessionCommandService;
import pe.upc.simutalk.services.InterviewSessionQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Interview Sessions", description = "Entrevista asincrónica: invitación, respuestas y cierre")
public class InterviewSessionController {

    private final InterviewSessionCommandService interviewSessionCommandService;
    private final InterviewSessionQueryService interviewSessionQueryService;

    @PostMapping("/applications/{applicationId}/interview-session")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @interviewsAccess.ownsApplicationJobPosting(#applicationId, authentication))")
    @Operation(summary = "Invitar a la entrevista",
            description = "La postulación debe estar en RECEIVED y pasa a INTERVIEWING. Una sesión por postulación.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sesión creada en PENDING"),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño ni admin"),
            @ApiResponse(responseCode = "404", description = "Postulación no encontrada"),
            @ApiResponse(responseCode = "422", description = "Postulación no RECEIVED, ya tiene sesión o guion vacío")
    })
    public ResponseEntity<InterviewSessionResource> createSession(@PathVariable Long applicationId,
                                                                  @Valid @RequestBody CreateInterviewSessionResource resource) {
        var session = interviewSessionCommandService.handle(
                CreateInterviewSessionCommandFromResourceAssembler.toCommandFromResource(applicationId, resource));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InterviewSessionResourceFromEntityAssembler.toResourceFromEntity(session));
    }

    @GetMapping("/applications/{applicationId}/interview-session")
    @PreAuthorize("hasRole('ADMIN') or @interviewsAccess.ownsApplicationJobPosting(#applicationId, authentication) "
            + "or @interviewsAccess.isApplicationCandidate(#applicationId, authentication)")
    @Operation(summary = "Ver la sesión de una postulación", description = "El recruiter dueño o el candidato de la postulación.")
    public ResponseEntity<InterviewSessionResource> getSessionByApplication(@PathVariable Long applicationId) {
        var session = interviewSessionQueryService.handle(new GetInterviewSessionByApplicationIdQuery(applicationId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application %s has no interview session".formatted(applicationId)));
        return ResponseEntity.ok(InterviewSessionResourceFromEntityAssembler.toResourceFromEntity(session));
    }

    @PostMapping("/interview-sessions/{sessionId}/start")
    @PreAuthorize("hasRole('CANDIDATE') and @interviewsAccess.isSessionCandidate(#sessionId, authentication)")
    @Operation(summary = "Iniciar la entrevista", description = "Solo el candidato dueño, desde PENDING y antes de que venza.")
    public ResponseEntity<InterviewSessionResource> startSession(@PathVariable Long sessionId) {
        var session = interviewSessionCommandService.handle(new StartInterviewSessionCommand(sessionId));
        return ResponseEntity.ok(InterviewSessionResourceFromEntityAssembler.toResourceFromEntity(session));
    }

    @PostMapping("/interview-sessions/{sessionId}/answers")
    @PreAuthorize("hasRole('CANDIDATE') and @interviewsAccess.isSessionCandidate(#sessionId, authentication)")
    @Operation(summary = "Registrar una respuesta",
            description = "Solo el candidato dueño y en IN_PROGRESS. Una respuesta por pregunta; a lo sumo una repregunta "
                    + "por pregunta, y solo si la pregunta lo permite.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Respuesta registrada"),
            @ApiResponse(responseCode = "403", description = "No es el candidato dueño"),
            @ApiResponse(responseCode = "422", description = "Sesión no IN_PROGRESS, pregunta ya respondida o repregunta inválida")
    })
    public ResponseEntity<AnswerResource> recordAnswer(@PathVariable Long sessionId,
                                                       @Valid @RequestBody RecordAnswerResource resource) {
        var answer = interviewSessionCommandService.handle(
                RecordAnswerCommandFromResourceAssembler.toCommandFromResource(sessionId, resource));
        return ResponseEntity.status(HttpStatus.CREATED).body(AnswerResourceFromEntityAssembler.toResourceFromEntity(answer));
    }

    @PostMapping("/interview-sessions/{sessionId}/completion")
    @PreAuthorize("hasRole('CANDIDATE') and @interviewsAccess.isSessionCandidate(#sessionId, authentication)")
    @Operation(summary = "Terminar la entrevista",
            description = "Solo el candidato dueño, con todas las preguntas respondidas. La postulación pasa a ASSESSED.")
    public ResponseEntity<InterviewSessionResource> completeSession(@PathVariable Long sessionId) {
        var session = interviewSessionCommandService.handle(new CompleteInterviewSessionCommand(sessionId));
        return ResponseEntity.ok(InterviewSessionResourceFromEntityAssembler.toResourceFromEntity(session));
    }

    @GetMapping("/interview-sessions/{sessionId}/answers")
    @PreAuthorize("hasRole('ADMIN') or @interviewsAccess.isSessionCandidate(#sessionId, authentication) "
            + "or @interviewsAccess.ownsSessionJobPosting(#sessionId, authentication)")
    @Operation(summary = "Ver las respuestas", description = "El candidato dueño o el recruiter de la vacante.")
    public ResponseEntity<List<AnswerResource>> getAnswers(@PathVariable Long sessionId) {
        var answers = interviewSessionQueryService.handle(new GetAnswersByInterviewSessionIdQuery(sessionId));
        return ResponseEntity.ok(answers.stream().map(AnswerResourceFromEntityAssembler::toResourceFromEntity).toList());
    }
}
