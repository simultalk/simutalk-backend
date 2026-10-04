package pe.upc.simutalk.securities;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceImplTest {

    private static final String SECRET = randomSecret();

    private final TokenServiceImpl tokenService = new TokenServiceImpl(SECRET, 7);

    @Test
    void generatedTokenIsValidAndCarriesTheUsername() {
        var token = tokenService.generateToken("ana");

        assertThat(tokenService.validateToken(token)).isTrue();
        assertThat(tokenService.getUsernameFromToken(token)).isEqualTo("ana");
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        var foreignToken = new TokenServiceImpl(randomSecret(), 7).generateToken("ana");

        assertThat(tokenService.validateToken(foreignToken)).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        var token = tokenService.generateToken("ana");
        var parts = token.split("\\.");
        var forgedPayload = Encoders.BASE64URL.encode("{\"sub\":\"admin\"}".getBytes());

        assertThat(tokenService.validateToken(parts[0] + "." + forgedPayload + "." + parts[2])).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        var past = Instant.now().minus(2, ChronoUnit.DAYS);
        var expired = Jwts.builder()
                .subject("ana")
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plus(1, ChronoUnit.DAYS)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();

        assertThat(tokenService.validateToken(expired)).isFalse();
    }

    @Test
    void malformedEmptyOrNullTokensAreRejectedWithoutThrowing() {
        assertThat(tokenService.validateToken("not-a-jwt")).isFalse();
        assertThat(tokenService.validateToken("")).isFalse();
        assertThat(tokenService.validateToken(null)).isFalse();
    }

    @Test
    void rejectsSecretsShorterThan256Bits() {
        var weak = Encoders.BASE64.encode("short-secret".getBytes());

        assertThatThrownBy(() -> new TokenServiceImpl(weak, 7)).isInstanceOf(WeakKeyException.class);
        assertThatThrownBy(() -> new TokenServiceImpl("", 7)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void extractsBearerTokenFromAuthorizationHeader() {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc.def.ghi");

        assertThat(tokenService.getBearerTokenFrom(request)).isEqualTo("abc.def.ghi");
    }

    @Test
    void returnsNullWhenHeaderIsMissingOrNotBearer() {
        var noHeader = new MockHttpServletRequest();
        var basic = new MockHttpServletRequest();
        basic.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        var emptyBearer = new MockHttpServletRequest();
        emptyBearer.addHeader("Authorization", "Bearer ");

        assertThat(tokenService.getBearerTokenFrom(noHeader)).isNull();
        assertThat(tokenService.getBearerTokenFrom(basic)).isNull();
        assertThat(tokenService.getBearerTokenFrom(emptyBearer)).isNull();
    }

    private static String randomSecret() {
        return Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
    }
}
