package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.Roles;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private final User user = new User("  ana.torres ", "$2a$10$hash");

    @Test
    void stripsUsernameAndStartsWithoutRoles() {
        assertThat(user.getUsername()).isEqualTo("ana.torres");
        assertThat(user.getRoles()).isEmpty();
    }

    @Test
    void rejectsBlankUsernameOrPassword() {
        assertThatThrownBy(() -> new User(" ", "hash")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User("ana", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User("a".repeat(51), "hash")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addRolesRejectsEmptyList() {
        assertThatThrownBy(() -> user.addRoles(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one role");
    }

    @Test
    void addRolesRejectsNull() {
        assertThatThrownBy(() -> user.addRoles(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addRolesIgnoresDuplicates() {
        user.addRoles(List.of(new Role(Roles.ROLE_RECRUITER), new Role(Roles.ROLE_RECRUITER)));
        user.addRole(new Role(Roles.ROLE_CANDIDATE));

        assertThat(user.getRoleNames()).containsExactly("ROLE_CANDIDATE", "ROLE_RECRUITER");
    }

    @Test
    void rolesAreReturnedImmutable() {
        user.addRole(Role.getDefaultRole());

        assertThatThrownBy(() -> user.getRoles().add(new Role(Roles.ROLE_ADMIN)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void signUpCannotGrantAdmin() {
        assertThatThrownBy(() -> user.addSignUpRoles(List.of(new Role(Roles.ROLE_CANDIDATE), new Role(Roles.ROLE_ADMIN))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("ROLE_ADMIN");
        assertThat(user.getRoles()).isEmpty();
    }

    @Test
    void signUpGrantsSelfAssignableRoles() {
        user.addSignUpRoles(List.of(new Role(Roles.ROLE_RECRUITER)));

        assertThat(user.getRoleNames()).containsExactly("ROLE_RECRUITER");
    }

    @Test
    void signUpRejectsEmptyRoles() {
        assertThatThrownBy(() -> user.addSignUpRoles(List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
