package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.AuthenticatedUserResource;
import pe.upc.simutalk.dtos.SignInResource;
import pe.upc.simutalk.dtos.SignUpResource;
import pe.upc.simutalk.dtos.UserResource;
import pe.upc.simutalk.exceptions.InvalidCredentialsException;
import pe.upc.simutalk.mappers.AuthenticatedUserResourceFromEntityAssembler;
import pe.upc.simutalk.mappers.SignInCommandFromResourceAssembler;
import pe.upc.simutalk.mappers.SignUpCommandFromResourceAssembler;
import pe.upc.simutalk.mappers.UserResourceFromEntityAssembler;
import pe.upc.simutalk.services.UserCommandService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Authentication", description = "Autenticación y registro")
public class AuthenticationController {

    private final UserCommandService userCommandService;

    @PostMapping("/sign-up")
    @Operation(summary = "Registrar un usuario",
            description = "Roles permitidos: ROLE_CANDIDATE, ROLE_RECRUITER. Sin roles se asigna ROLE_CANDIDATE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o rol desconocido"),
            @ApiResponse(responseCode = "422", description = "Username ya existe o rol no permitido")
    })
    public ResponseEntity<UserResource> signUp(@Valid @RequestBody SignUpResource resource) {
        var command = SignUpCommandFromResourceAssembler.toCommandFromResource(resource);
        var user = userCommandService.handle(command)
                .orElseThrow(() -> new IllegalStateException("Sign-up did not return a user"));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResourceFromEntityAssembler.toResourceFromEntity(user));
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Iniciar sesión", description = "Devuelve el JWT para el header Authorization: Bearer <token>.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public ResponseEntity<AuthenticatedUserResource> signIn(@Valid @RequestBody SignInResource resource) {
        var command = SignInCommandFromResourceAssembler.toCommandFromResource(resource);
        var authenticatedUser = userCommandService.handle(command).orElseThrow(InvalidCredentialsException::new);
        return ResponseEntity.ok(AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(authenticatedUser));
    }
}
