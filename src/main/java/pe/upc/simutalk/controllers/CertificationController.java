package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.CertificationResource;
import pe.upc.simutalk.dtos.CertificationVerificationResource;
import pe.upc.simutalk.dtos.CreateCertificationResource;
import pe.upc.simutalk.dtos.DeleteCertificationCommand;
import pe.upc.simutalk.dtos.GetCertificationsByCandidateIdQuery;
import pe.upc.simutalk.dtos.VerifyCertificationCommand;
import pe.upc.simutalk.mappers.AddCertificationCommandFromResourceAssembler;
import pe.upc.simutalk.mappers.CertificationResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.CertificationVerificationResourceFromEntityAssembler;
import pe.upc.simutalk.services.CandidateProfileCommandService;
import pe.upc.simutalk.services.CandidateProfileQueryService;

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
@RequestMapping(value = "/api/v1/candidate-profiles/{candidateId}/certifications", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Certifications", description = "Certificaciones del postulante y su verificación")
public class CertificationController {

    private final CandidateProfileCommandService candidateProfileCommandService;
    private final CandidateProfileQueryService candidateProfileQueryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @profileAccess.ownsCandidate(#candidateId, authentication)")
    @Operation(summary = "Agregar certificación", description = "Siempre nace UNVERIFIED.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Certificación agregada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Sin permiso"),
            @ApiResponse(responseCode = "422", description = "Credencial ya registrada")
    })
    public ResponseEntity<CertificationResource> addCertification(@PathVariable Long candidateId,
                                                                  @Valid @RequestBody CreateCertificationResource resource) {
        var certification = candidateProfileCommandService.handle(
                AddCertificationCommandFromResourceAssembler.toCommandFromResource(candidateId, resource));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{certificationId}").buildAndExpand(certification.getId()).toUri();
        return ResponseEntity.created(location)
                .body(CertificationResourceFromEntityAssembler.toResourceFromEntity(certification));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER') or @profileAccess.ownsCandidate(#candidateId, authentication)")
    @Operation(summary = "Listar certificaciones")
    public ResponseEntity<List<CertificationResource>> getCertifications(@PathVariable Long candidateId) {
        var certifications = candidateProfileQueryService.handle(new GetCertificationsByCandidateIdQuery(candidateId));
        return ResponseEntity.ok(certifications.stream()
                .map(CertificationResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    @PostMapping("/{certificationId}/verification")
    @PreAuthorize("hasRole('ADMIN') or @profileAccess.ownsCandidate(#candidateId, authentication)")
    @Operation(summary = "Verificar certificación con el emisor",
            description = "Coincide → VERIFIED; no coincide → REJECTED; emisor no disponible → sigue UNVERIFIED. "
                    + "Sin código de credencial responde 422.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verificación procesada"),
            @ApiResponse(responseCode = "404", description = "Certificación no encontrada"),
            @ApiResponse(responseCode = "422", description = "Sin código de credencial o ya verificada/rechazada")
    })
    public ResponseEntity<CertificationVerificationResource> verifyCertification(@PathVariable Long candidateId,
                                                                                 @PathVariable Long certificationId) {
        var verification = candidateProfileCommandService.handle(
                new VerifyCertificationCommand(candidateId, certificationId));
        return ResponseEntity.ok(CertificationVerificationResourceFromEntityAssembler.toResourceFromEntity(verification));
    }

    @DeleteMapping("/{certificationId}")
    @PreAuthorize("hasRole('ADMIN') or @profileAccess.ownsCandidate(#candidateId, authentication)")
    @Operation(summary = "Eliminar certificación")
    public ResponseEntity<Void> deleteCertification(@PathVariable Long candidateId, @PathVariable Long certificationId) {
        candidateProfileCommandService.handle(new DeleteCertificationCommand(candidateId, certificationId));
        return ResponseEntity.noContent().build();
    }
}
