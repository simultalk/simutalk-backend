package pe.upc.simutalk.serviceimpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Transport to the generative AI provider (Google Gemini {@code generateContent}). Pure infrastructure,
 * with no domain knowledge: it receives a prompt and returns the text the model produced. It does
 * not know what a criterion or a score is; each bounded context keeps its own port and adapter,
 * builds its own prompt and validates the answer against its own schema.
 * <p>
 * It concentrates what every adapter would otherwise repeat:
 * <ul>
 *   <li>{@code external.ai.mode}: {@code mock} (default, never touches the network: adapters answer
 *       with their own deterministic logic) or {@code live} (requires {@code GEMINI_API_KEY});</li>
 *   <li>connect/read timeout, bounded retries with exponential backoff on HTTP 429/503;</li>
 *   <li>an LRU cache keyed by the SHA-256 of model + prompt;</li>
 *   <li>an empty result instead of an exception when the provider fails.</li>
 * </ul>
 * Privacy (Ley 29733): anonymization happens in each context's adapter BEFORE the prompt reaches
 * this client, never here. This client sends exactly the prompt it receives.
 */
@Slf4j
@Component
public class GenerativeAiClient {

    enum Mode { MOCK, LIVE }

    private final Mode mode;
    private final String model;
    private final String apiKey;
    private final int maxRetries;
    private final Duration initialBackoff;
    private final RestClient restClient;
    private final Map<String, String> cache;

    @Autowired
    public GenerativeAiClient(@Value("${external.ai.mode:mock}") String mode,
                              @Value("${external.ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
                              @Value("${external.ai.gemini.model:gemini-2.5-flash}") String model,
                              @Value("${external.ai.gemini.api-key:}") String apiKey,
                              @Value("${external.ai.timeout:20s}") Duration timeout,
                              @Value("${external.ai.max-retries:3}") int maxRetries,
                              @Value("${external.ai.initial-backoff:1s}") Duration initialBackoff,
                              @Value("${external.ai.cache-size:500}") int cacheSize,
                              RestClient.Builder restClientBuilder) {
        this.mode = parseMode(mode);
        this.model = model;
        this.apiKey = apiKey;
        this.maxRetries = maxRetries;
        this.initialBackoff = initialBackoff;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > cacheSize;
            }
        });
        if (this.mode == Mode.LIVE && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalStateException("external.ai.mode=live requires GEMINI_API_KEY");
        }
        log.info("Generative AI client running in {} mode{}", this.mode, this.mode == Mode.LIVE ? " (" + model + ")" : "");
    }

    /** {@code false} in mock mode: adapters must answer with their own logic and never call {@link #generate}. */
    public boolean isLive() {
        return mode == Mode.LIVE;
    }

    public String model() {
        return model;
    }

    /**
     * Sends the prompt and returns the model's text answer (JSON when the prompt asks for it).
     * Empty in mock mode, when the provider fails after the retries, or when it answers nothing.
     */
    public Optional<String> generate(String prompt) {
        if (mode != Mode.LIVE || prompt == null || prompt.isBlank()) {
            return Optional.empty();
        }
        var key = cacheKey(prompt);
        var cached = cache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        var text = callWithResilience(() -> request(prompt));
        text.ifPresent(value -> cache.put(key, value));
        return text;
    }

    /** Body of a {@code generateContent} request that asks for a JSON answer. */
    public static Map<String, Object> requestBody(String prompt) {
        return Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", "application/json"));
    }

    private String request(String prompt) {
        var response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody(prompt))
                .retrieve()
                .body(JsonNode.class);
        return response == null ? ""
                : response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asString("");
    }

    /**
     * Exponential backoff on 429/503 and an empty fallback; the timeouts live in {@link #restClient}.
     * Package visible so the policy can be tested without network.
     */
    Optional<String> callWithResilience(Supplier<String> call) {
        var backoff = initialBackoff;
        for (var attempt = 0; ; attempt++) {
            try {
                var text = call.get();
                return text == null || text.isBlank() ? Optional.empty() : Optional.of(text);
            } catch (RuntimeException ex) {
                if (!isRetryable(ex) || attempt >= maxRetries) {
                    log.warn("Generative AI provider unavailable: {}", ex.toString());
                    return Optional.empty();
                }
            }
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
            backoff = backoff.multipliedBy(2);
        }
    }

    private String cacheKey(String prompt) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest((model + "\u0000" + prompt).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static boolean isRetryable(RuntimeException ex) {
        return ex instanceof RestClientResponseException response
                && (response.getStatusCode().value() == 429 || response.getStatusCode().value() == 503);
    }

    private static Mode parseMode(String mode) {
        try {
            return Mode.valueOf(mode.strip().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("external.ai.mode must be 'mock' or 'live', got: " + mode);
        }
    }
}
