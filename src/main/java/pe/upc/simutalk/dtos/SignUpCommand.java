package pe.upc.simutalk.dtos;

import pe.upc.simutalk.entities.Role;

import java.util.List;

/**
 * @param roles requested roles; an empty list means the default role
 */
public record SignUpCommand(String username, String password, List<Role> roles) {

    public SignUpCommand {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
