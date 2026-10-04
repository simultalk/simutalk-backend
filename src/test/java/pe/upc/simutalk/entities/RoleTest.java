package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.Roles;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleTest {

    @Test
    void defaultRoleIsCandidate() {
        assertThat(Role.getDefaultRole().getName()).isEqualTo(Roles.ROLE_CANDIDATE);
    }

    @Test
    void buildsRoleFromNameIgnoringCaseAndSpaces() {
        var role = Role.toRoleFromName(" role_recruiter ");

        assertThat(role.getName()).isEqualTo(Roles.ROLE_RECRUITER);
        assertThat(role.getStringName()).isEqualTo("ROLE_RECRUITER");
    }

    @Test
    void rejectsUnknownRoleName() {
        assertThatThrownBy(() -> Role.toRoleFromName("ROLE_ROOT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown role");
    }

    @Test
    void onlyAdminIsNotSelfAssignable() {
        assertThat(new Role(Roles.ROLE_ADMIN).isSelfAssignable()).isFalse();
        assertThat(new Role(Roles.ROLE_RECRUITER).isSelfAssignable()).isTrue();
        assertThat(new Role(Roles.ROLE_CANDIDATE).isSelfAssignable()).isTrue();
    }

    @Test
    void rolesWithSameNameAreEqual() {
        assertThat(new Role(Roles.ROLE_ADMIN)).isEqualTo(new Role(Roles.ROLE_ADMIN)).hasSameHashCodeAs(new Role(Roles.ROLE_ADMIN));
    }
}
