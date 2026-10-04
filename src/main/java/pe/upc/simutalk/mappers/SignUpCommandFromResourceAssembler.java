package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.SignUpCommand;
import pe.upc.simutalk.dtos.SignUpResource;
import pe.upc.simutalk.entities.Role;

import java.util.List;

public class SignUpCommandFromResourceAssembler {

    public static SignUpCommand toCommandFromResource(SignUpResource resource) {
        var roles = resource.roles() == null
                ? List.<Role>of()
                : resource.roles().stream().map(Role::toRoleFromName).toList();
        return new SignUpCommand(resource.username(), resource.password(), roles);
    }
}
