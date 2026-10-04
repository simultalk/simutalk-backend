package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.GetAllRolesQuery;
import pe.upc.simutalk.dtos.RoleResource;
import pe.upc.simutalk.mappers.RoleResourceFromEntityAssembler;
import pe.upc.simutalk.services.RoleQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/roles", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Roles", description = "Catálogo de roles")
public class RoleController {

    private final RoleQueryService roleQueryService;

    @GetMapping
    @Operation(summary = "Listar roles")
    public ResponseEntity<List<RoleResource>> getAllRoles() {
        var roles = roleQueryService.handle(new GetAllRolesQuery());
        return ResponseEntity.ok(roles.stream().map(RoleResourceFromEntityAssembler::toResourceFromEntity).toList());
    }
}
