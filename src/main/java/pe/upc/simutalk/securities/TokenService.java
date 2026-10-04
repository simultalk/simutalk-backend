package pe.upc.simutalk.securities;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Access token issuing and validation as seen by the application layer.
 */
public interface TokenService {

    String generateToken(String username);

    String getUsernameFromToken(String token);

    /**
     * @return {@code true} if the token is well formed, correctly signed and not expired.
     *         Never throws.
     */
    boolean validateToken(String token);

    /**
     * @return the token in the {@code Authorization: Bearer <token>} header, or {@code null}
     */
    String getBearerTokenFrom(HttpServletRequest request);
}
