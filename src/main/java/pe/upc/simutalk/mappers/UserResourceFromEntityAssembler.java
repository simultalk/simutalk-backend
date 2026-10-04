package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.UserResource;
import pe.upc.simutalk.entities.User;

public class UserResourceFromEntityAssembler {

    public static UserResource toResourceFromEntity(User user) {
        return new UserResource(user.getId(), user.getUsername(), user.getRoleNames());
    }
}
