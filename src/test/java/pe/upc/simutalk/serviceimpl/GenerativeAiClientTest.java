package pe.upc.simutalk.serviceimpl;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerativeAiClientTest {

    private static GenerativeAiClient client(String mode, String baseUrl, String key) {
        return new GenerativeAiClient(mode, baseUrl, "gemini-test", key, Duration.ofMillis(300), 3,
                Duration.ofMillis(5), 10, RestClient.builder());
    }

    private static GenerativeAiClient live() {
        return client("live", "https://generativelanguage.googleapis.com", "k");
    }

    @Test
    void mockModeNeverAnswersSoAdaptersUseTheirOwnLogic() {
        var mock = client("mock", "http://127.0.0.1:1", "");

        assertThat(mock.isLive()).isFalse();
        assertThat(mock.generate("cualquier prompt")).isEmpty();
    }

    @Test
    void liveModeRequiresAnApiKey() {
        assertThatThrownBy(() -> client("live", "http://127.0.0.1:1", " ")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> client("sandbox", "http://127.0.0.1:1", "k")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void retriesOnRateLimitWithBackoffThenSucceeds() {
        var attempts = new AtomicInteger();

        var text = live().callWithResilience(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, new byte[0], null);
            }
            return "{\"ok\":true}";
        });

        assertThat(text).contains("{\"ok\":true}");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void givesUpAfterTheRetriesAndOnOtherErrors() {
        var attempts = new AtomicInteger();
        var exhausted = live().callWithResilience(() -> {
            attempts.incrementAndGet();
            throw HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, new byte[0], null);
        });
        var other = new AtomicInteger();
        var failed = live().callWithResilience(() -> {
            other.incrementAndGet();
            throw new IllegalStateException("boom");
        });

        assertThat(exhausted).isEmpty();
        assertThat(attempts).hasValue(4);
        assertThat(failed).isEmpty();
        assertThat(other).hasValue(1);
    }

    @Test
    void sendsThePromptAsIsExtractsTheTextAndCachesIt() throws Exception {
        var calls = new AtomicInteger();
        var sentBody = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            sentBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            var response = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"answer\\\":42}\"}]}}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var client = client("live", "http://127.0.0.1:" + server.getAddress().getPort(), "k");

            var first = client.generate("PROMPT [NOMBRE]");
            var second = client.generate("PROMPT [NOMBRE]");

            assertThat(first).contains("{\"answer\":42}");
            assertThat(second).isEqualTo(first);
            assertThat(calls).hasValue(1);
            assertThat(sentBody.get()).contains("PROMPT [NOMBRE]", "\"responseMimeType\":\"application/json\"");
        } finally {
            server.stop(0);
        }
    }
}
