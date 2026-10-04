package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.AuthenticatedUser;
import pe.upc.simutalk.dtos.SeedAdminUserCommand;
import pe.upc.simutalk.dtos.SignInCommand;
import pe.upc.simutalk.dtos.SignUpCommand;
import pe.upc.simutalk.entities.User;

import java.util.Optional;

public interface UserCommandService {

    Optional<User> handle(SignUpCommand command);

    Optional<AuthenticatedUser> handle(SignInCommand command);

    void handle(SeedAdminUserCommand command);
}
