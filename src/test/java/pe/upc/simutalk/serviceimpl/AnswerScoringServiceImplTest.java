package pe.upc.simutalk.serviceimpl;

import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import pe.upc.simutalk.enums.CriterionKind;
import pe.upc.simutalk.services.AnswerScoringService.CriterionFeedback;

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
    void fallsBackToTheMockScorerWhenTheProviderDoesNotAnswerWithinTheTimeout() throws Exception {
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

            assertThat(result.isAvailable()).isTrue();
            assertThat(result).isEqualTo(MockAnswerScorer.score(TRANSCRIPT, "Pensamiento analítico", "Resuelve problemas con datos."));
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

    /* ---------- quality of the score ---------- */

    private static final String CRITERION = "Pensamiento analítico";
    private static final String CRITERION_DESCRIPTION = "Descompone problemas de negocio y los resuelve con datos.";

    @Test
    void mockScorerRanksGoodAboveWeakAboveEvasive() {
        var good = MockAnswerScorer.score(TRANSCRIPT, CRITERION, CRITERION_DESCRIPTION);
        var weak = MockAnswerScorer.score("Me gustan los datos y creo que soy analítico.", CRITERION, CRITERION_DESCRIPTION);
        var evasive = MockAnswerScorer.score("No sé, la verdad no sé cómo responder.", CRITERION, CRITERION_DESCRIPTION);

        assertThat(evasive.score()).isLessThanOrEqualTo(new BigDecimal("2.0"));
        assertThat(weak.score()).isGreaterThan(evasive.score());
        assertThat(good.score()).isGreaterThan(weak.score());
        assertThat(evasive.isAvailable()).isTrue();
        assertThat(evasive.rationale()).contains("evasiva");
    }

    @Test
    void mockScorerGivesALowScoreToAnEmptyAnswer() {
        var empty = MockAnswerScorer.score("Bueno, eso.", CRITERION, CRITERION_DESCRIPTION);

        assertThat(empty.score()).isLessThanOrEqualTo(new BigDecimal("2.0"));
    }

    @Test
    void aLongRelevantAnswerIsNotTakenAsEvasive() {
        var answer = "No se puede analizar sin datos limpios, así que primero descompondría el problema de negocio "
                + "por región y canal, revisaría los datos de ventas y validaría la hipótesis con el equipo comercial.";

        var result = MockAnswerScorer.score(answer, CRITERION, CRITERION_DESCRIPTION);

        assertThat(result.score()).isGreaterThan(new BigDecimal("5.0"));
        assertThat(result.rationale()).isNull();
    }

    @Test
    void readsTheRationaleAndCapsItsLength() {
        var live = service("live", "k");
        var response = "{\"score\":1.0,\"confidence\":0.9,\"excerpt\":\"Primero confirmaría que la caída es real.\","
                + "\"rationale\":\"" + "x".repeat(400) + "\"}";

        var result = live.parse(response, TRANSCRIPT);

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.rationale()).hasSize(AnswerScoringServiceImpl.RATIONALE_MAX_LENGTH);
    }

    @Test
    void theScoringPromptPenalizesEvasiveAnswersAndAsksForAStrictJson() {
        var prompt = GeminiRequestFactory.prompt("No sé, la verdad no sé cómo responder.", CRITERION, CRITERION_DESCRIPTION);

        assertThat(prompt).contains("0 a 2", "evasiva", "\"score\"", "\"excerpt\"", "\"rationale\"", "LITERALMENTE",
                "No sé, la verdad no sé cómo responder.");
    }

    @Test
    void theFeedbackPromptExplainsWhyWithTheRationale() {
        var prompt = GeminiRequestFactory.feedbackPrompt("1.0", List.of(new CriterionFeedback(CRITERION,
                CriterionKind.COMPETENCY, new BigDecimal("1.0"), 100, "No sé", "Respuesta evasiva: no aporta evidencia.")));

        assertThat(prompt).contains("motivo del puntaje: Respuesta evasiva: no aporta evidencia.", "evasiva");
    }

    /* ---------- feedback without markers, rationale in the requested language ---------- */

    @Test
    void theFeedbackNeverShowsAnonymizationMarkers() {
        assertThat(AnswerScoringServiceImpl.withoutMarkers("Estimado/a [NOMBRE], gracias por tu tiempo. Vives en [DIRECCION]."))
                .isEqualTo("Estimado/a, gracias por tu tiempo. Vives en.");
        assertThat(AnswerScoringServiceImpl.withoutMarkers("  Gracias por completar la entrevista.  "))
                .isEqualTo("Gracias por completar la entrevista.");
    }

    @Test
    void theFeedbackPromptForbidsGreetingByNameAndFollowsTheLanguage() {
        var prompt = GeminiRequestFactory.feedbackPrompt("1.0", List.of(new CriterionFeedback(CRITERION,
                CriterionKind.COMPETENCY, new BigDecimal("1.0"), 100, "No sé", null)));

        assertThat(prompt).contains("No saludes por nombre", "en el idioma").doesNotContain("un texto en español");
    }

    @Test
    void mockRationaleFollowsTheRequestLanguage() {
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-US");
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(request));
        try {
            var evasive = MockAnswerScorer.score("No sé, la verdad no sé cómo responder.", CRITERION, CRITERION_DESCRIPTION);

            assertThat(evasive.rationale()).startsWith("Evasive answer");
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }
}
