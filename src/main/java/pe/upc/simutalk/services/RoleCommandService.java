package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.SeedRolesCommand;

public interface RoleCommandService {

    void handle(SeedRolesCommand command);
}
