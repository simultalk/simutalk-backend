package pe.upc.simutalk.dtos;

import pe.upc.simutalk.entities.User;

/**
 * Result of a successful sign-in: the user and the token issued for it.
 */
public record AuthenticatedUser(User user, String token) {

    public AuthenticatedUser {
        if (user == null || token == null || token.isBlank()) {
            throw new IllegalArgumentException("Authenticated user requires a user and a token");
        }
    }
}
