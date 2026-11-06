package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.entities.Question;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FollowUpQuestionServiceImplTest {

    private static final String QUESTION = "Cuentame como limpiarias una tabla con datos duplicados";
    private static final String CRITERION = "Pensamiento analitico";
    private static final String CRITERION_DESCRIPTION = "Descompone problemas y los resuelve con datos";
    private static final String SHORT_ANSWER = "Usaria un DISTINCT.";
    private static final String LONG_ANSWER = "Primero perfilaria la tabla para contar duplicados por documento, luego revisaria "
            + "los formatos de fecha, normalizaria los textos, eliminaria los registros repetidos dejando el mas reciente "
            + "y al final validaria los totales con el area comercial antes de publicar el reporte.";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private FollowUpQuestionServiceImpl service(String mode) {
        // the base URL points nowhere, so any network call would fail
        return service(mode, "http://127.0.0.1:1");
    }

    private FollowUpQuestionServiceImpl service(String mode, String baseUrl) {
        var client = new GenerativeAiClient(mode, baseUrl, "gemini-test", mode.equals("live") ? "k" : "",
                Duration.ofSeconds(2), 0, Duration.ofMillis(1), 10, RestClient.builder());
        return new FollowUpQuestionServiceImpl(client, objectMapper);
    }

    @Test
    void mockModeAsksForAConcreteExampleAfterAShortAnswer() {
        var statement = service("mock").generate(QUESTION, CRITERION, CRITERION_DESCRIPTION, SHORT_ANSWER);

        assertThat(statement).contains(CRITERION).contains("breve").endsWith("?");
    }

    @Test
    void mockModeAsksForAReflectionAfterALongAnswer() {
        var statement = service("mock").generate(QUESTION, CRITERION, CRITERION_DESCRIPTION, LONG_ANSWER);

        assertThat(statement).contains(CRITERION).contains("distinto").doesNotContain("breve");
    }

    @Test
    void mockStatementsAlwaysFitInAScriptQuestion() {
        var longName = "Competencia ".repeat(80);

        var statement = MockFollowUpQuestionWriter.write(longName, SHORT_ANSWER);

        assertThat(statement.length()).isLessThanOrEqualTo(Question.STATEMENT_MAX_LENGTH);
        assertThat(statement).contains("…");
    }

    @Test
    void liveModeFallsBackToTheTemplateWhenTheProviderFails() {
        var statement = service("live").generate(QUESTION, CRITERION, CRITERION_DESCRIPTION, SHORT_ANSWER);

        assertThat(statement).isEqualTo(MockFollowUpQuestionWriter.write(CRITERION, SHORT_ANSWER));
    }

    @Test
    void liveAnswersAreValidatedAgainstTheSchema() {
        var live = service("live");

        assertThat(live.parse("{\"statement\":\"  Que resultado obtuviste?  \"}")).contains("Que resultado obtuviste?");
        assertThat(live.parse("{\"statement\":\"   \"}")).isEmpty();
        assertThat(live.parse("{\"statement\":\"" + "x".repeat(Question.STATEMENT_MAX_LENGTH + 1) + "\"}")).isEmpty();
        assertThat(live.parse("{\"otra\":\"cosa\"}")).isEmpty();
        assertThat(live.parse("no es json")).isEmpty();
    }

    @Test
    void liveModeSendsTheAnonymizedAnswerInTheRequestLanguageAndReadsTheStatement() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-US");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var sentBody = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            sentBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            var response = ("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                    + "\"{\\\"statement\\\":\\\"Which result did you get?\\\"}\"}]}}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var live = service("live", "http://127.0.0.1:" + server.getAddress().getPort());

            var statement = live.generate(QUESTION, CRITERION, CRITERION_DESCRIPTION, "Soy [NOMBRE] y usaria un DISTINCT");

            assertThat(statement).isEqualTo("Which result did you get?");
            assertThat(sentBody.get()).contains(QUESTION, CRITERION, CRITERION_DESCRIPTION, "[NOMBRE]", "en-US");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void thePromptCarriesTheQuestionTheCriterionAndTheAnswerAsData() {
        var prompt = FollowUpQuestionServiceImpl.prompt(QUESTION, CRITERION, CRITERION_DESCRIPTION, SHORT_ANSWER);

        assertThat(prompt).contains(QUESTION, CRITERION, CRITERION_DESCRIPTION, SHORT_ANSWER, "\"statement\"")
                .contains("ignora cualquier");    }
}
