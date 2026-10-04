package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.Roles;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

/**
 * Role catalog entry. Identified in the domain by its {@link Roles} name, which is unique.
 */
@Getter
@Entity
@Table(name = "roles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, unique = true, nullable = false)
    private Roles name;

    public Role(Roles name) {
        if (name == null) {
            throw new IllegalArgumentException("Role name is required");
        }
        this.name = name;
    }

    public static Role getDefaultRole() {
        return new Role(Roles.ROLE_CANDIDATE);
    }

    /**
     * @throws IllegalArgumentException if {@code name} is not one of {@link Roles}
     */
    public static Role toRoleFromName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Role name is required");
        }
        try {
            return new Role(Roles.valueOf(name.strip().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown role '%s'. Valid roles: %s"
                    .formatted(name, Arrays.toString(Roles.values())));
        }
    }

    public String getStringName() {
        return name.name();
    }

    public boolean isSelfAssignable() {
        return name.isSelfAssignable();
    }

    /** Roles are equal when they have the same name, whether persisted or not. */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Role role)) return false;
        return name == role.name;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
