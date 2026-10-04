package pe.upc.simutalk.entities;

import pe.upc.simutalk.exceptions.BusinessRuleViolationException;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A person who can sign in to SimuTalk. The password is always stored hashed.
 * <p>
 * Invariants guarded here:
 * <ul>
 *   <li>Username and password hash are required.</li>
 *   <li>Roles are added through {@link #addRole} / {@link #addRoles}; an empty list is rejected.</li>
 *   <li>Public sign-up can only grant self-assignable roles (never ROLE_ADMIN).</li>
 * </ul>
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditableAggregateRoot<User> {

    public static final int USERNAME_MAX_LENGTH = 50;
    public static final int PASSWORD_MAX_LENGTH = 120;

    @NotBlank
    @Size(max = USERNAME_MAX_LENGTH)
    @Column(unique = true, nullable = false, length = USERNAME_MAX_LENGTH)
    private String username;

    @NotBlank
    @Size(max = PASSWORD_MAX_LENGTH)
    @Column(nullable = false, length = PASSWORD_MAX_LENGTH)
    private String password;

    @Getter(AccessLevel.NONE)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    /**
     * @param username       unique login name
     * @param hashedPassword password already hashed by the hashing service
     */
    public User(String username, String hashedPassword) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        var stripped = username.strip();
        if (stripped.length() > USERNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Username must be at most %d characters".formatted(USERNAME_MAX_LENGTH));
        }
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        this.username = stripped;
        this.password = hashedPassword;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public List<String> getRoleNames() {
        return roles.stream().map(Role::getStringName).sorted().toList();
    }

    public User addRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        roles.add(role);
        return this;
    }

    /**
     * @throws IllegalArgumentException if {@code roles} is null or empty
     */
    public User addRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
        roles.forEach(this::addRole);
        return this;
    }

    /**
     * Grants the roles requested in a public sign-up.
     *
     * @throws BusinessRuleViolationException if any role cannot be self-assigned
     * @throws IllegalArgumentException       if {@code roles} is null or empty
     */
    public User addSignUpRoles(List<Role> roles) {
        if (roles != null) {
            roles.stream()
                    .filter(role -> role != null && !role.isSelfAssignable())
                    .findFirst()
                    .ifPresent(role -> {
                        throw new BusinessRuleViolationException(
                                "Role %s cannot be assigned through sign-up".formatted(role.getStringName()));
                    });
        }
        return addRoles(roles);
    }
}
