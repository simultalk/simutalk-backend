package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.CreateQuestionResource;
import pe.upc.simutalk.dtos.DeleteQuestionCommand;
import pe.upc.simutalk.dtos.GetQuestionSuggestionsQuery;
import pe.upc.simutalk.dtos.GetQuestionsByJobPostingIdQuery;
import pe.upc.simutalk.dtos.QuestionResource;
import pe.upc.simutalk.dtos.QuestionSuggestionResource;
import pe.upc.simutalk.dtos.ReorderQuestionsResource;
import pe.upc.simutalk.dtos.UpdateQuestionResource;
import pe.upc.simutalk.mappers.CreateQuestionCommandFromResourceAssembler;
import pe.upc.simutalk.mappers.QuestionResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.QuestionSuggestionResourceFromValueAssembler;
import pe.upc.simutalk.mappers.ReorderQuestionsCommandFromResourceAssembler;
import pe.upc.simutalk.mappers.UpdateQuestionCommandFromResourceAssembler;
import pe.upc.simutalk.services.QuestionCommandService;
import pe.upc.simutalk.services.QuestionQueryService;
import pe.upc.simutalk.services.QuestionSuggestionQueryService;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/job-postings/{jobPostingId}/questions", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Interview Questions", description = "Guion de preguntas de la entrevista de una vacante")
public class QuestionController {

    private final QuestionCommandService questionCommandService;
    private final QuestionQueryService questionQueryService;
    private final QuestionSuggestionQueryService questionSuggestionQueryService;

    @PostMapping("/suggestions")
    @PreAuthorize("hasRole('RECRUITER') and @interviewsAccess.ownsJobPosting(#jobPostingId, authentication)")
    @Operation(summary = "Sugerir preguntas para un criterio",
            description = "Solo el recruiter dueño y con la vacante en DRAFT. Devuelve preguntas propuestas para el "
                    + "criterio COMPETENCY indicado SIN persistirlas: el reclutador acepta las que quiera con "
                    + "POST /questions, poniendo él el tiempo de respuesta y origin=AI_SUGGESTED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preguntas propuestas"),
            @ApiResponse(responseCode = "400", description = "Falta criterionId o no es un número"),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño de la vacante"),
            @ApiResponse(responseCode = "404", description = "Vacante no encontrada"),
            @ApiResponse(responseCode = "422", description = "Vacante no DRAFT, criterio de otra vacante o de CERTIFICATION")
    })
    public ResponseEntity<List<QuestionSuggestionResource>> suggestQuestions(
            @PathVariable Long jobPostingId,
            @Parameter(description = "Criterio COMPETENCY de la misma vacante") @RequestParam Long criterionId) {
        var suggestions = questionSuggestionQueryService.handle(new GetQuestionSuggestionsQuery(jobPostingId, criterionId));
        return ResponseEntity.ok(suggestions.stream()
                .map(suggestion -> QuestionSuggestionResourceFromValueAssembler.toResourceFromValue(criterionId, suggestion))
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @interviewsAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Agregar pregunta al guion",
            description = "Solo con la vacante en DRAFT y sobre un criterio COMPETENCY de la misma vacante. Se agrega al final.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pregunta creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No es el recruiter dueño ni admin"),
            @ApiResponse(responseCode = "422", description = "Vacante no DRAFT, criterio de otra vacante o de CERTIFICATION")
    })
    public ResponseEntity<QuestionResource> createQuestion(@PathVariable Long jobPostingId,
                                                           @Valid @RequestBody CreateQuestionResource resource) {
        var question = questionCommandService.handle(
                CreateQuestionCommandFromResourceAssembler.toCommandFromResource(jobPostingId, resource));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{questionId}").buildAndExpand(question.getId()).toUri();
        return ResponseEntity.created(location).body(QuestionResourceFromEntityAssembler.toResourceFromEntity(question));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @interviewsAccess.canReadScript(#jobPostingId, authentication)")
    @Operation(summary = "Ver el guion",
            description = "El recruiter dueño, o el candidato con una entrevista IN_PROGRESS en esta vacante.")
    public ResponseEntity<List<QuestionResource>> getQuestions(@PathVariable Long jobPostingId) {
        var questions = questionQueryService.handle(new GetQuestionsByJobPostingIdQuery(jobPostingId));
        return ResponseEntity.ok(questions.stream().map(QuestionResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    /* "/order" is declared before "/{questionId}" so the literal path reads clearly first. */
    @PatchMapping("/order")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @interviewsAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Reordenar el guion", description = "Lista con todos los ids del guion en el nuevo orden. Solo en DRAFT.")
    public ResponseEntity<List<QuestionResource>> reorderQuestions(@PathVariable Long jobPostingId,
                                                                   @Valid @RequestBody ReorderQuestionsResource resource) {
        var questions = questionCommandService.handle(
                ReorderQuestionsCommandFromResourceAssembler.toCommandFromResource(jobPostingId, resource));
        return ResponseEntity.ok(questions.stream().map(QuestionResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    @PutMapping("/{questionId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @interviewsAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Editar pregunta", description = "Solo en DRAFT. La posición cambia con PATCH /order.")
    public ResponseEntity<QuestionResource> updateQuestion(@PathVariable Long jobPostingId, @PathVariable Long questionId,
                                                           @Valid @RequestBody UpdateQuestionResource resource) {
        var question = questionCommandService.handle(
                UpdateQuestionCommandFromResourceAssembler.toCommandFromResource(jobPostingId, questionId, resource));
        return ResponseEntity.ok(QuestionResourceFromEntityAssembler.toResourceFromEntity(question));
    }

    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @interviewsAccess.ownsJobPosting(#jobPostingId, authentication))")
    @Operation(summary = "Eliminar pregunta", description = "Solo en DRAFT. Las posiciones se renumeran.")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long jobPostingId, @PathVariable Long questionId) {
        questionCommandService.handle(new DeleteQuestionCommand(jobPostingId, questionId));
        return ResponseEntity.noContent().build();
    }
}
