package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.SignInCommand;
import pe.upc.simutalk.dtos.SignUpCommand;
import pe.upc.simutalk.entities.Role;
import pe.upc.simutalk.entities.User;
import pe.upc.simutalk.enums.Roles;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.InvalidCredentialsException;
import pe.upc.simutalk.repositories.RoleRepository;
import pe.upc.simutalk.repositories.UserRepository;
import pe.upc.simutalk.securities.BCryptHashingServiceImpl;
import pe.upc.simutalk.securities.TokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserCommandServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final BCryptHashingServiceImpl hashingService = new BCryptHashingServiceImpl();
    private UserCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserCommandServiceImpl(userRepository, roleRepository, hashingService, tokenService);
        for (var name : Roles.values()) {
            when(roleRepository.findByName(name)).thenReturn(Optional.of(new Role(name)));
        }
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void signUpHashesPasswordAndAssignsDefaultRole() {
        var user = service.handle(new SignUpCommand("ana", "secret123", List.of())).orElseThrow();

        assertThat(user.getPassword()).isNotEqualTo("secret123");
        assertThat(hashingService.matches("secret123", user.getPassword())).isTrue();
        assertThat(user.getRoleNames()).containsExactly("ROLE_CANDIDATE");
    }

    @Test
    void signUpRejectsDuplicatedUsername() {
        when(userRepository.existsByUsername("ana")).thenReturn(true);

        assertThatThrownBy(() -> service.handle(new SignUpCommand("ana", "secret123", List.of())))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Username already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void signUpRejectsAdminRole() {
        assertThatThrownBy(() -> service.handle(new SignUpCommand("ana", "secret123", List.of(new Role(Roles.ROLE_ADMIN)))))
                .isInstanceOf(BusinessRuleViolationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void signUpRejectsRoleMissingInCatalog() {
        when(roleRepository.findByName(Roles.ROLE_RECRUITER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(new SignUpCommand("ana", "secret123", List.of(new Role(Roles.ROLE_RECRUITER)))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void signInReturnsTokenForValidCredentials() {
        var user = new User("ana", hashingService.encode("secret123"));
        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(user));
        when(tokenService.generateToken("ana")).thenReturn("jwt-token");

        var authenticated = service.handle(new SignInCommand("ana", "secret123")).orElseThrow();

        assertThat(authenticated.user()).isSameAs(user);
        assertThat(authenticated.token()).isEqualTo("jwt-token");
    }

    @Test
    void signInFailsWithSameErrorForWrongPasswordAndUnknownUser() {
        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(new User("ana", hashingService.encode("secret123"))));
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(new SignInCommand("ana", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
        assertThatThrownBy(() -> service.handle(new SignInCommand("nobody", "secret123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
        verify(tokenService, never()).generateToken(any());
    }
}
