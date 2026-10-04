package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.services.CredentialVerificationService.VerificationResult;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialVerificationServiceImplTest {

    private static CredentialVerificationServiceImpl service(String mode, Duration timeout) {
        return new CredentialVerificationServiceImpl(mode, timeout, 3, Duration.ofMillis(10), RestClient.builder());
    }

    private final CredentialVerificationServiceImpl mock = service("mock", Duration.ofSeconds(5));

    @Test
    void mockMatchesCodesWithEightOrMoreCharacters() {
        var result = mock.verify("Coursera", "ABCD1234", "Google Data Analytics", "Rosa Quispe");

        assertThat(result.matched()).isTrue();
        assertThat(result.conclusive()).isTrue();
    }

    @Test
    void mockDoesNotMatchShorterCodes() {
        var result = mock.verify("Credly", "ABC1234", "AWS", "Rosa Quispe");

        assertThat(result.matched()).isFalse();
        assertThat(result.conclusive()).isTrue();
    }

    @Test
    void mockIgnoresSurroundingSpaces() {
        assertThat(mock.verify("Credly", "  ABC1234  ", "AWS", "Rosa").matched()).isFalse();
    }

    @Test
    void missingCodeNeverMatches() {
        assertThat(mock.verify("CertiProf", null, "Scrum", "Rosa").matched()).isFalse();
        assertThat(mock.verify("CertiProf", " ", "Scrum", "Rosa").matched()).isFalse();
    }

    @Test
    void liveModeWithoutIssuerClientFallsBackToInconclusive() {
        var result = service("live", Duration.ofSeconds(5)).verify("Coursera", "ABCD1234", "Java", "Rosa");

        assertThat(result.matched()).isFalse();
        assertThat(result.conclusive()).isFalse();
    }

    @Test
    void retriesOnTooManyRequestsAndThenSucceeds() {
        var attempts = new AtomicInteger();
        var result = mock.callWithResilience(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw tooManyRequests();
            }
            return new VerificationResult(true, "ok");
        });

        assertThat(result.matched()).isTrue();
        assertThat(attempts).hasValue(3);
    }

    @Test
    void fallsBackWhenRetriesAreExhausted() {
        var attempts = new AtomicInteger();
        var result = mock.callWithResilience(() -> {
            attempts.incrementAndGet();
            throw tooManyRequests();
        });

        assertThat(result.conclusive()).isFalse();
        assertThat(attempts).hasValue(4);
    }

    @Test
    void doesNotRetryOtherErrorsAndFallsBack() {
        var attempts = new AtomicInteger();
        var result = mock.callWithResilience(() -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("boom");
        });

        assertThat(result.conclusive()).isFalse();
        assertThat(attempts).hasValue(1);
    }

    @Test
    void fallsBackOnTimeoutWithoutRetrying() {
        var attempts = new AtomicInteger();

        var result = mock.callWithResilience(() -> {
            attempts.incrementAndGet();
            throw new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out"));
        });

        assertThat(result.conclusive()).isFalse();
        assertThat(attempts).hasValue(1);
    }

    @Test
    void rejectsUnknownMode() {
        assertThatThrownBy(() -> service("sandbox", Duration.ofSeconds(1))).isInstanceOf(IllegalStateException.class);
    }

    private static HttpClientErrorException tooManyRequests() {
        return HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, new byte[0], null);
    }
}
