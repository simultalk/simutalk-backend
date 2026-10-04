package pe.upc.simutalk.securities;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT tokens signed with HS256. The secret is a Base64 string of at least 256 bits read
 * from {@code JWT_SECRET}; the application refuses to start with a weaker key.
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey signingKey;
    private final Duration expiration;

    public TokenServiceImpl(@Value("${authorization.jwt.secret}") String secret,
                            @Value("${authorization.jwt.expiration-days}") int expirationDays) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("authorization.jwt.secret (JWT_SECRET) must be set");
        }
        if (expirationDays <= 0) {
            throw new IllegalStateException("authorization.jwt.expiration-days must be positive");
        }
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiration = Duration.ofDays(expirationDays);
    }

    @Override
    public String generateToken(String username) {
        var issuedAt = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(expiration)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.info("Expired JWT: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Empty JWT: {}", ex.getMessage());
        } catch (JwtException ex) {
            log.warn("Invalid JWT: {}", ex.getMessage());
        }
        return false;
    }

    @Override
    public String getBearerTokenFrom(HttpServletRequest request) {
        var header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        var token = header.substring(BEARER_PREFIX.length()).strip();
        return token.isEmpty() ? null : token;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
