package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.GetAllUsersQuery;
import pe.upc.simutalk.dtos.GetUserByIdQuery;
import pe.upc.simutalk.dtos.GetUserByUsernameQuery;
import pe.upc.simutalk.entities.User;

import java.util.List;
import java.util.Optional;

public interface UserQueryService {

    List<User> handle(GetAllUsersQuery query);

    Optional<User> handle(GetUserByIdQuery query);

    Optional<User> handle(GetUserByUsernameQuery query);
}
