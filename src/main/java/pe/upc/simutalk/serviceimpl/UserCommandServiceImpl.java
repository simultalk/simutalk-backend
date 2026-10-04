package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.AuthenticatedUser;
import pe.upc.simutalk.dtos.SeedAdminUserCommand;
import pe.upc.simutalk.dtos.SignInCommand;
import pe.upc.simutalk.dtos.SignUpCommand;
import pe.upc.simutalk.entities.Role;
import pe.upc.simutalk.entities.User;
import pe.upc.simutalk.enums.Roles;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.InvalidCredentialsException;
import pe.upc.simutalk.repositories.RoleRepository;
import pe.upc.simutalk.repositories.UserRepository;
import pe.upc.simutalk.securities.HashingService;
import pe.upc.simutalk.securities.TokenService;
import pe.upc.simutalk.services.UserCommandService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;

    /**
     * Compared against when the username does not exist, so a failed sign-in costs the
     * same time whether or not the user exists.
     */
    private final String dummyPasswordHash;

    public UserCommandServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                                  HashingService hashingService, TokenService tokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.dummyPasswordHash = hashingService.encode("simutalk-dummy-password");
    }

    @Override
    public Optional<User> handle(SignUpCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new BusinessRuleViolationException("Username already exists");
        }
        var requestedRoles = command.roles().isEmpty() ? List.of(Role.getDefaultRole()) : command.roles();
        var roles = requestedRoles.stream().map(this::resolveRole).toList();

        var user = new User(command.username(), hashingService.encode(command.password()));
        user.addSignUpRoles(roles);
        return Optional.of(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> handle(SignInCommand command) {
        var user = userRepository.findByUsername(command.username());
        if (user.isEmpty()) {
            hashingService.matches(command.password(), dummyPasswordHash);
            throw new InvalidCredentialsException();
        }
        if (!hashingService.matches(command.password(), user.get().getPassword())) {
            throw new InvalidCredentialsException();
        }
        var token = tokenService.generateToken(user.get().getUsername());
        return Optional.of(new AuthenticatedUser(user.get(), token));
    }

    @Override
    public void handle(SeedAdminUserCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            log.info("Bootstrap admin '{}' already exists; left unchanged", command.username());
            return;
        }
        var adminRole = roleRepository.findByName(Roles.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN must be seeded before the admin user"));
        var admin = new User(command.username(), hashingService.encode(command.password()));
        admin.addRole(adminRole);
        userRepository.save(admin);
        log.info("Bootstrap admin '{}' created", command.username());
    }

    private Role resolveRole(Role requested) {
        return roleRepository.findByName(requested.getName())
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Role %s does not exist".formatted(requested.getStringName())));
    }
}
