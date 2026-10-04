package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.services.CredentialVerificationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Adapter for {@link CredentialVerificationService}.
 * <p>
 * Mode {@code external.credentials.mode}:
 * <ul>
 *   <li>{@code mock} (default): no network. A code of 8 or more characters matches,
 *       a shorter one does not.</li>
 *   <li>{@code live}: calls the issuer through {@link RestClient}. No issuer is wired yet
 *       (see TODOs), so every live request answers "unavailable" and the certification
 *       stays UNVERIFIED.</li>
 * </ul>
 * Every live call has connect and read timeouts, retries with exponential backoff on HTTP 429
 * and falls back to an inconclusive result instead of throwing.
 */
@Slf4j
@Service
public class CredentialVerificationServiceImpl implements CredentialVerificationService {

    static final int MOCK_MIN_CODE_LENGTH = 8;

    enum Mode { MOCK, LIVE }

    private final Mode mode;
    private final int maxRetries;
    private final Duration initialBackoff;
    /** HTTP client for the issuer calls registered in {@link #issuerClients}; carries the timeouts. */
    private final RestClient restClient;

    /**
     * Issuer name (lower case) -> call that verifies a credential with that issuer.
     * TODO(coursera): wire the official verification API once access is granted; do not guess endpoints.
     * TODO(credly): wire the official Credly badge verification API once access is granted.
     * TODO(certiprof): wire CertiProf's verification service once its API is documented.
     */
    private final Map<String, Function<CredentialRequest, VerificationResult>> issuerClients = Map.of();

    public CredentialVerificationServiceImpl(
            @Value("${external.credentials.mode:mock}") String mode,
            @Value("${external.credentials.timeout:5s}") Duration timeout,
            @Value("${external.credentials.max-retries:3}") int maxRetries,
            @Value("${external.credentials.initial-backoff:500ms}") Duration initialBackoff,
            RestClient.Builder restClientBuilder) {
        this.mode = parseMode(mode);
        this.maxRetries = maxRetries;
        this.initialBackoff = initialBackoff;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        log.info("Credential verification running in {} mode", this.mode);
    }

    @Override
    public VerificationResult verify(String issuer, String credentialCode, String title, String holderName) {
        if (credentialCode == null || credentialCode.isBlank()) {
            return new VerificationResult(false, "No credential code to verify");
        }
        return switch (mode) {
            case MOCK -> verifyMock(credentialCode);
            case LIVE -> verifyLive(new CredentialRequest(issuer, credentialCode.strip(), title, holderName));
        };
    }

    private VerificationResult verifyMock(String credentialCode) {
        return credentialCode.strip().length() >= MOCK_MIN_CODE_LENGTH
                ? new VerificationResult(true, "Mock: credential code accepted")
                : new VerificationResult(false, "Mock: credential code too short (min %d characters)"
                .formatted(MOCK_MIN_CODE_LENGTH));
    }

    private VerificationResult verifyLive(CredentialRequest request) {
        var issuerKey = request.issuer() == null ? "" : request.issuer().strip().toLowerCase(Locale.ROOT);
        var client = issuerClients.get(issuerKey);
        if (client == null) {
            return VerificationResult.unavailable(
                    "Live verification is not available yet for issuer '%s'".formatted(request.issuer()));
        }
        return callWithResilience(() -> client.apply(request));
    }

    /**
     * Exponential backoff on HTTP 429 and an inconclusive fallback; the timeouts live in
     * {@link #restClient}. Package visible so the policy can be tested without network.
     */
    VerificationResult callWithResilience(Supplier<VerificationResult> call) {
        var backoff = initialBackoff;
        for (var attempt = 0; ; attempt++) {
            try {
                var result = call.get();
                return result != null ? result : VerificationResult.unavailable("Issuer returned no answer");
            } catch (RuntimeException ex) {
                if (!isTooManyRequests(ex) || attempt >= maxRetries) {
                    log.warn("Credential verification unavailable: {}", ex.toString());
                    return VerificationResult.unavailable("Issuer unavailable; try again later");
                }
            }
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return VerificationResult.unavailable("Issuer unavailable; try again later");
            }
            backoff = backoff.multipliedBy(2);
        }
    }

    private static boolean isTooManyRequests(RuntimeException ex) {
        return ex instanceof RestClientResponseException response
                && response.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value();
    }

    private static Mode parseMode(String mode) {
        try {
            return Mode.valueOf(mode.strip().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("external.credentials.mode must be 'mock' or 'live', got: " + mode);
        }
    }

    record CredentialRequest(String issuer, String credentialCode, String title, String holderName) {
    }
}
