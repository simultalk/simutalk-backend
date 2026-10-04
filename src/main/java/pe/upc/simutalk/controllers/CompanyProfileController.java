package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.CompanyProfileResource;
import pe.upc.simutalk.dtos.CreateCompanyProfileResource;
import pe.upc.simutalk.dtos.GetAllCompanyProfilesQuery;
import pe.upc.simutalk.dtos.GetCompanyProfileByIdQuery;
import pe.upc.simutalk.dtos.GetCompanyProfileByUserIdQuery;
import pe.upc.simutalk.dtos.PageResource;
import pe.upc.simutalk.dtos.UpdateCompanyProfileResource;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.mappers.CompanyProfileResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.CreateCompanyProfileCommandFromResourceAssembler;
import pe.upc.simutalk.mappers.UpdateCompanyProfileCommandFromResourceAssembler;
import pe.upc.simutalk.securities.ProfileAccessPolicy;
import pe.upc.simutalk.services.CompanyProfileCommandService;
import pe.upc.simutalk.services.CompanyProfileQueryService;

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
@RequestMapping(value = "/api/v1/company-profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Company Profiles", description = "Perfiles de empresa")
public class CompanyProfileController {

    private final CompanyProfileCommandService companyProfileCommandService;
    private final CompanyProfileQueryService companyProfileQueryService;
    private final ProfileAccessPolicy profileAccessPolicy;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECRUITER') and @profileAccess.isCurrentUser(#resource.userId, authentication))")
    @Operation(summary = "Crear perfil de empresa", description = "Un recruiter crea el suyo; un admin, cualquiera.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Perfil creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (p. ej. RUC)"),
            @ApiResponse(responseCode = "403", description = "Sin permiso"),
            @ApiResponse(responseCode = "422", description = "Usuario inexistente, con perfil o RUC duplicado")
    })
    public ResponseEntity<CompanyProfileResource> createCompanyProfile(
            @Valid @RequestBody CreateCompanyProfileResource resource) {
        var company = companyProfileCommandService.handle(
                CreateCompanyProfileCommandFromResourceAssembler.toCommandFromResource(resource));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(company.getId()).toUri();
        return ResponseEntity.created(location).body(CompanyProfileResourceFromEntityAssembler.toResourceFromEntity(company));
    }

    /* "/me" is declared before "/{companyProfileId}" to make the intent explicit. */
    @GetMapping("/me")
    @Operation(summary = "Mi perfil de empresa", description = "Perfil del usuario autenticado; 404 si no tiene.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "404", description = "El usuario autenticado no tiene perfil de empresa")
    })
    public ResponseEntity<CompanyProfileResource> getMyCompanyProfile() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var company = profileAccessPolicy.findCurrentUserId(authentication)
                .flatMap(userId -> companyProfileQueryService.handle(new GetCompanyProfileByUserIdQuery(userId)))
                .orElseThrow(() -> new ResourceNotFoundException("The authenticated user has no company profile"));
        return ResponseEntity.ok(CompanyProfileResourceFromEntityAssembler.toResourceFromEntity(company));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar perfiles de empresa", description = "Solo ROLE_ADMIN. Paginado (page desde 0).")
    public ResponseEntity<PageResource<CompanyProfileResource>> getAllCompanyProfiles(
            @Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Tamaño de página, 1 a 100") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var companies = companyProfileQueryService.handle(new GetAllCompanyProfilesQuery(page, size));
        return ResponseEntity.ok(PageResource.from(companies, CompanyProfileResourceFromEntityAssembler::toResourceFromEntity));
    }

    @GetMapping("/{companyProfileId}")
    @Operation(summary = "Obtener perfil de empresa")
    public ResponseEntity<CompanyProfileResource> getCompanyProfileById(@PathVariable Long companyProfileId) {
        var company = companyProfileQueryService.handle(new GetCompanyProfileByIdQuery(companyProfileId))
                .orElseThrow(() -> new ResourceNotFoundException("Company profile", companyProfileId));
        return ResponseEntity.ok(CompanyProfileResourceFromEntityAssembler.toResourceFromEntity(company));
    }

    @PutMapping("/{companyProfileId}")
    @PreAuthorize("hasRole('ADMIN') or @profileAccess.ownsCompany(#companyProfileId, authentication)")
    @Operation(summary = "Actualizar perfil de empresa", description = "El RUC no se puede cambiar.")
    public ResponseEntity<CompanyProfileResource> updateCompanyProfile(
            @PathVariable Long companyProfileId, @Valid @RequestBody UpdateCompanyProfileResource resource) {
        var company = companyProfileCommandService.handle(
                UpdateCompanyProfileCommandFromResourceAssembler.toCommandFromResource(companyProfileId, resource));
        return ResponseEntity.ok(CompanyProfileResourceFromEntityAssembler.toResourceFromEntity(company));
    }
}
