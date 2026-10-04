package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.AuthenticatedUser;
import pe.upc.simutalk.dtos.AuthenticatedUserResource;

public class AuthenticatedUserResourceFromEntityAssembler {

    public static AuthenticatedUserResource toResourceFromEntity(AuthenticatedUser authenticatedUser) {
        var user = authenticatedUser.user();
        return new AuthenticatedUserResource(user.getId(), user.getUsername(), authenticatedUser.token());
    }
}
