package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.GetUserByIdQuery;
import pe.upc.simutalk.dtos.GetUserByUsernameQuery;
import pe.upc.simutalk.dtos.SignUpCommand;
import pe.upc.simutalk.entities.User;
import pe.upc.simutalk.services.UserCommandService;
import pe.upc.simutalk.services.UserQueryService;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IamContextFacadeImplTest {

    private final UserQueryService userQueryService = mock(UserQueryService.class);
    private final UserCommandService userCommandService = mock(UserCommandService.class);
    private final IamContextFacadeImpl facade = new IamContextFacadeImpl(userQueryService, userCommandService);

    @Test
    void resolvesExistingUser() {
        var user = new User("ana", "hash");
        ReflectionTestUtils.setField(user, "id", 7L);
        when(userQueryService.handle(new GetUserByUsernameQuery("ana"))).thenReturn(Optional.of(user));
        when(userQueryService.handle(new GetUserByIdQuery(7L))).thenReturn(Optional.of(user));

        assertThat(facade.fetchUserIdByUsername("ana")).isEqualTo(7L);
        assertThat(facade.fetchUsernameByUserId(7L)).isEqualTo("ana");
        assertThat(facade.existsUserById(7L)).isTrue();
    }

    @Test
    void signUpUserIfAbsentReusesExistingUser() {
        var user = new User("rosa", "hash");
        ReflectionTestUtils.setField(user, "id", 8L);
        when(userQueryService.handle(new GetUserByUsernameQuery("rosa"))).thenReturn(Optional.of(user));

        assertThat(facade.signUpUserIfAbsent("rosa", "Passw0rd!", "ROLE_CANDIDATE")).isEqualTo(8L);
        verifyNoInteractions(userCommandService);
    }

    @Test
    void signUpUserIfAbsentCreatesMissingUser() {
        var user = new User("rosa", "hash");
        ReflectionTestUtils.setField(user, "id", 9L);
        when(userQueryService.handle(new GetUserByUsernameQuery("rosa"))).thenReturn(Optional.empty());
        when(userCommandService.handle(any(SignUpCommand.class))).thenReturn(Optional.of(user));

        assertThat(facade.signUpUserIfAbsent("rosa", "Passw0rd!", "ROLE_CANDIDATE")).isEqualTo(9L);
    }

    @Test
    void returnsNeutralValuesForMissingUser() {
        when(userQueryService.handle(any(GetUserByUsernameQuery.class))).thenReturn(Optional.empty());
        when(userQueryService.handle(any(GetUserByIdQuery.class))).thenReturn(Optional.empty());

        assertThat(facade.fetchUserIdByUsername("nobody")).isZero();
        assertThat(facade.fetchUsernameByUserId(99L)).isEmpty();
        assertThat(facade.existsUserById(99L)).isFalse();
        assertThat(facade.existsUserById(null)).isFalse();
    }
}
