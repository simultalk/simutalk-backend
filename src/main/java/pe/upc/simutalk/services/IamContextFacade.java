package pe.upc.simutalk.services;

/**
 * Contract other bounded contexts use to ask {@code iam} about users.
 * <p>
 * It lives in {@code shared} so that callers (e.g. {@code profiles}, {@code recruitment})
 * depend only on {@code shared}; {@code iam} provides the implementation. Never use
 * iam's repositories or domain classes from another context.
 */
public interface IamContextFacade {

    /**
     * @return the user id, or {@code 0L} if no user has that username
     */
    Long fetchUserIdByUsername(String username);

    /**
     * @return the username, or an empty string if the user does not exist
     */
    String fetchUsernameByUserId(Long userId);

    boolean existsUserById(Long userId);

    /**
     * Registers a user through the public sign-up rules (never ROLE_ADMIN) unless the
     * username already exists. Meant for seeding demo data from other contexts.
     *
     * @return the id of the new or existing user
     */
    Long signUpUserIfAbsent(String username, String password, String roleName);
}
