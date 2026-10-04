package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.CandidateProfileResource;
import pe.upc.simutalk.dtos.CreateCandidateProfileResource;
import pe.upc.simutalk.dtos.GetAllCandidateProfilesQuery;
import pe.upc.simutalk.dtos.GetCandidateProfileByIdQuery;
import pe.upc.simutalk.dtos.GetCandidateProfileByUserIdQuery;
import pe.upc.simutalk.dtos.PageResource;
import pe.upc.simutalk.dtos.UpdateCandidateProfileResource;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.mappers.CandidateProfileResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.CreateCandidateProfileCommandFromResourceAssembler;
import pe.upc.simutalk.mappers.UpdateCandidateProfileCommandFromResourceAssembler;
import pe.upc.simutalk.securities.ProfileAccessPolicy;
import pe.upc.simutalk.services.CandidateProfileCommandService;
import pe.upc.simutalk.services.CandidateProfileQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/api/v1/candidate-profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Candidate Profiles", description = "Perfiles de postulante")
public class CandidateProfileController {

    private final CandidateProfileCommandService candidateProfileCommandService;
    private final CandidateProfileQueryService candidateProfileQueryService;
    private final ProfileAccessPolicy profileAccessPolicy;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CANDIDATE') and @profileAccess.isCurrentUser(#resource.userId, authentication))")
    @Operation(summary = "Crear perfil de postulante", description = "Un candidato crea el suyo; un admin, cualquiera.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Perfil creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (menor de 18, documento, experiencia)"),
            @ApiResponse(responseCode = "403", description = "Sin permiso"),
            @ApiResponse(responseCode = "422", description = "Usuario inexistente, con perfil o documento duplicado")
    })
    public ResponseEntity<CandidateProfileResource> createCandidateProfile(
            @Valid @RequestBody CreateCandidateProfileResource resource) {
        var candidate = candidateProfileCommandService.handle(
                CreateCandidateProfileCommandFromResourceAssembler.toCommandFromResource(resource));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(candidate.getId()).toUri();
        return ResponseEntity.created(location)
                .body(CandidateProfileResourceFromEntityAssembler.toResourceFromEntity(candidate));
    }

    /* "/me" is declared before "/{candidateId}". Spring MVC would still prefer the literal
       path over the variable, but keeping the order makes the intent explicit. */
    @GetMapping("/me")
    @Operation(summary = "Mi perfil de postulante", description = "Perfil del usuario autenticado; 404 si no tiene.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "404", description = "El usuario autenticado no tiene perfil de postulante")
    })
    public ResponseEntity<CandidateProfileResource> getMyCandidateProfile() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var candidate = profileAccessPolicy.findCurrentUserId(authentication)
                .flatMap(userId -> candidateProfileQueryService.handle(new GetCandidateProfileByUserIdQuery(userId)))
                .orElseThrow(() -> new ResourceNotFoundException("The authenticated user has no candidate profile"));
        return ResponseEntity.ok(CandidateProfileResourceFromEntityAssembler.toResourceFromEntity(candidate));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar perfiles de postulante", description = "Solo ROLE_ADMIN. Paginado (page desde 0).")
    public ResponseEntity<PageResource<CandidateProfileResource>> getAllCandidateProfiles(
            @Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Tamaño de página, 1 a 100") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var candidates = candidateProfileQueryService.handle(new GetAllCandidateProfilesQuery(page, size));
        return ResponseEntity.ok(PageResource.from(candidates, CandidateProfileResourceFromEntityAssembler::toResourceFromEntity));
    }

    @GetMapping("/{candidateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER') or @profileAccess.ownsCandidate(#candidateId, authentication)")
    @Operation(summary = "Obtener perfil de postulante", description = "Admin, recruiter o el propio candidato.")
    public ResponseEntity<CandidateProfileResource> getCandidateProfileById(@PathVariable Long candidateId) {
        var candidate = candidateProfileQueryService.handle(new GetCandidateProfileByIdQuery(candidateId))
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile", candidateId));
        return ResponseEntity.ok(CandidateProfileResourceFromEntityAssembler.toResourceFromEntity(candidate));
    }

    @PutMapping("/{candidateId}")
    @PreAuthorize("hasRole('ADMIN') or @profileAccess.ownsCandidate(#candidateId, authentication)")
    @Operation(summary = "Actualizar perfil de postulante",
            description = "Solo el propio candidato o un admin. El documento no se puede cambiar.")
    public ResponseEntity<CandidateProfileResource> updateCandidateProfile(
            @PathVariable Long candidateId, @Valid @RequestBody UpdateCandidateProfileResource resource) {
        var candidate = candidateProfileCommandService.handle(
                UpdateCandidateProfileCommandFromResourceAssembler.toCommandFromResource(candidateId, resource));
        return ResponseEntity.ok(CandidateProfileResourceFromEntityAssembler.toResourceFromEntity(candidate));
    }
}
