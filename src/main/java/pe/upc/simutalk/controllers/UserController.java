package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.GetAllUsersQuery;
import pe.upc.simutalk.dtos.GetUserByIdQuery;
import pe.upc.simutalk.dtos.UserResource;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.mappers.UserResourceFromEntityAssembler;
import pe.upc.simutalk.services.UserQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "Consulta de usuarios")
public class UserController {

    private final UserQueryService userQueryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuarios", description = "Solo ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios"),
            @ApiResponse(responseCode = "401", description = "Sin token válido"),
            @ApiResponse(responseCode = "403", description = "No es administrador")
    })
    public ResponseEntity<List<UserResource>> getAllUsers() {
        var users = userQueryService.handle(new GetAllUsersQuery());
        return ResponseEntity.ok(users.stream().map(UserResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Obtener un usuario", description = "ROLE_ADMIN o el propio usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "401", description = "Sin token válido"),
            @ApiResponse(responseCode = "403", description = "No es administrador ni el propio usuario"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UserResource> getUserById(@PathVariable Long userId) {
        var user = userQueryService.handle(new GetUserByIdQuery(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user));
    }
}
