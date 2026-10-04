package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.GetAllRolesQuery;
import pe.upc.simutalk.dtos.GetRoleByNameQuery;
import pe.upc.simutalk.entities.Role;

import java.util.List;
import java.util.Optional;

public interface RoleQueryService {

    List<Role> handle(GetAllRolesQuery query);

    Optional<Role> handle(GetRoleByNameQuery query);
}
