package pe.upc.simutalk.serviceimpl;

import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerScoringServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AnswerScoringServiceImpl service(String mode, String key) {
        return service(mode, "https://generativelanguage.googleapis.com", key);
    }

    private AnswerScoringServiceImpl service(String mode, String baseUrl, String key) {
        return new AnswerScoringServiceImpl(mode, baseUrl, "gemini-test", key,
                Duration.ofMillis(200), 3, Duration.ofMillis(5), 10, RestClient.builder(), objectMapper);
    }

    private static final String TRANSCRIPT = "Primero confirmaría que la caída es real. Luego separaría el volumen del ticket "
            + "promedio y descompondría los datos por región y canal para encontrar el problema de negocio.";

    @Test
    void mockModeIsDeterministicAndAnchoredInTheText() {
        var mock = service("mock", "");

        var first = mock.score(TRANSCRIPT, "Pensamiento analítico", "Descompone problemas de negocio y los resuelve con datos.");
        var second = mock.score(TRANSCRIPT, "Pensamiento analítico", "Descompone problemas de negocio y los resuelve con datos.");

        assertThat(first).isEqualTo(second);
        assertThat(first.isAvailable()).isTrue();
        assertThat(TRANSCRIPT.substring(first.startOffset(), first.endOffset())).isEqualTo(first.excerpt());
        assertThat(first.excerpt()).contains("descompondría los datos");
        assertThat(first.score()).isBetween(BigDecimal.ZERO, BigDecimal.TEN);
        assertThat(mock.engineVersion()).isEqualTo("mock-1");
    }

    @Test
    void liveModeRequiresAnApiKey() {
        assertThatThrownBy(() -> service("live", " ")).isInstanceOf(IllegalStateException.class);
        assertThat(service("live", "k").engineVersion()).isEqualTo("gemini:gemini-test");
    }

    @Test
    void fallsBackWhenTheProviderDoesNotAnswerWithinTheTimeout() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.start();
        try {
            var live = service("live", "http://127.0.0.1:" + server.getAddress().getPort(), "k");
            var startedAt = System.nanoTime();

            var result = live.score(TRANSCRIPT, "Pensamiento analítico", "Resuelve problemas con datos.");

            assertThat(result.isAvailable()).isFalse();
            assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofMillis(1_500));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void discardsExcerptsThatAreNotLiteralFragments() {
        var live = service("live", "k");
        var response = "{\"score\":9,\"confidence\":0.9,\"excerpt\":\"texto inventado por el modelo\"}";

        assertThat(live.parse(response, TRANSCRIPT).isAvailable()).isFalse();
    }

    @Test
    void clampsScoresAndLocatesTheExcerpt() {
        var live = service("live", "k");
        var response = "{\"score\":12,\"confidence\":1.4,\"excerpt\":\"separaría el volumen del ticket promedio\","
                + "\"aiGeneratedSuspicion\":true}";

        var result = live.parse(response, TRANSCRIPT);

        assertThat(result.score()).isEqualByComparingTo("10.0");
        assertThat(result.confidence()).isEqualByComparingTo("1.00");
        assertThat(result.aiGeneratedSuspicion()).isTrue();
        assertThat(TRANSCRIPT.substring(result.startOffset(), result.endOffset())).isEqualTo(result.excerpt());
    }
}
