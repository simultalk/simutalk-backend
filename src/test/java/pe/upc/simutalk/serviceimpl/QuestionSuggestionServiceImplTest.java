package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.entities.Question;
import pe.upc.simutalk.services.QuestionSuggestionService;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionSuggestionServiceImplTest {

    private static final String JOB_TITLE = "Analista de Datos Junior";
    private static final String JOB_DESCRIPTION = "Analisis de datos comerciales con SQL y Excel.";
    private static final String CRITERION = "Pensamiento analítico";
    private static final String CRITERION_DESCRIPTION = "Descompone problemas y los resuelve apoyándose en datos.";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private QuestionSuggestionServiceImpl service(String mode) {
        // the base URL points nowhere, so any network call would fail
        return service(mode, "http://127.0.0.1:1");
    }

    private QuestionSuggestionServiceImpl service(String mode, String baseUrl) {
        var client = new GenerativeAiClient(mode, baseUrl, "gemini-test", mode.equals("live") ? "k" : "",
                Duration.ofSeconds(2), 0, Duration.ofMillis(1), 10, RestClient.builder());
        return new QuestionSuggestionServiceImpl(client, objectMapper);
    }

    @Test
    void mockModeReturnsThreeQuestionsDerivedFromTheCriterionNameAndDescription() {
        var suggestions = service("mock").suggest(JOB_TITLE, JOB_DESCRIPTION, CRITERION, CRITERION_DESCRIPTION, List.of());

        assertThat(suggestions).hasSize(3);
        assertThat(suggestions).allSatisfy(suggestion -> assertThat(suggestion.statement()).contains(CRITERION));
        assertThat(suggestions.get(1).statement()).contains("Descompone problemas y los resuelve apoyándose en datos");
        assertThat(suggestions).allSatisfy(suggestion -> assertThat(suggestion.rationale()).isNotBlank());
    }

    @Test
    void mockModeIsDeterministicAndSkipsQuestionsTheScriptAlreadyHas() {
        var mock = service("mock");
        var accepted = mock.suggest(JOB_TITLE, JOB_DESCRIPTION, CRITERION, CRITERION_DESCRIPTION, List.of())
                .get(0).statement();

        var first = mock.suggest(JOB_TITLE, JOB_DESCRIPTION, CRITERION, CRITERION_DESCRIPTION, List.of(accepted.toUpperCase()));
        var second = mock.suggest(JOB_TITLE, JOB_DESCRIPTION, CRITERION, CRITERION_DESCRIPTION, List.of(accepted.toUpperCase()));

        assertThat(first).isEqualTo(second).hasSize(3);
        assertThat(first).extracting("statement").doesNotContain(accepted);
    }

    @Test
    void mockStatementsAlwaysFitInAScriptQuestion() {
        var longName = "N".repeat(100);
        var longDescription = "palabra ".repeat(125);

        var suggestions = service("mock").suggest(JOB_TITLE, JOB_DESCRIPTION, longName, longDescription, List.of());

        assertThat(suggestions).hasSize(3);
        assertThat(suggestions).allSatisfy(suggestion ->
                assertThat(suggestion.statement().length()).isLessThanOrEqualTo(Question.STATEMENT_MAX_LENGTH));
    }

    @Test
    void liveAnswersAreValidatedAgainstTheSchema() {
        var tooLong = "x".repeat(Question.STATEMENT_MAX_LENGTH + 1);
        var answer = """
                [{"statement":"Describe un analisis que hayas hecho con datos incompletos.","rationale":"Experiencia","maxDurationSeconds":9999},
                 {"statement":"  describe un ANALISIS que hayas hecho con datos incompletos. "},
                 {"statement":"Ya esta en el guion"},
                 {"statement":""},
                 {"rationale":"sin pregunta"},
                 {"statement":"%s"},
                 {"statement":"Como validas un resultado antes de presentarlo?"},
                 {"statement":"Que harias si dos fuentes de datos se contradicen?"},
                 {"statement":"Una cuarta pregunta que ya no entra"}]""".formatted(tooLong);

        var suggestions = service("live").parse(answer, List.of("ya está en el guion"));

        assertThat(suggestions).extracting("statement").containsExactly(
                "Describe un analisis que hayas hecho con datos incompletos.",
                "Como validas un resultado antes de presentarlo?",
                "Que harias si dos fuentes de datos se contradicen?");
        assertThat(suggestions.get(0).rationale()).isEqualTo("Experiencia");
    }

    @Test
    void liveAnswersMayComeWrappedAndUnreadableOnesGiveNothing() {
        var live = service("live");

        assertThat(live.parse("{\"questions\":[{\"statement\":\"Como priorizas?\"}]}", List.of())).hasSize(1);
        assertThat(live.parse("esto no es JSON", List.of())).isEmpty();
        assertThat(live.parse("{\"otra\":\"cosa\"}", List.of())).isEmpty();
    }

    @Test
    void liveModeFallsBackToTheTemplateQuestionsWhenTheProviderFails() {
        var suggestions = service("live").suggest(JOB_TITLE, JOB_DESCRIPTION, CRITERION, CRITERION_DESCRIPTION, List.of());

        assertThat(suggestions).hasSize(3);
        assertThat(suggestions.get(0).statement()).contains(CRITERION);
    }

    @Test
    void liveModeSendsOnlyThePostingAndCriterionTextAndReadsTheQuestions() throws Exception {
        var sentBody = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            sentBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            var response = ("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                    + "\"[{\\\"statement\\\":\\\"Como priorizas un analisis urgente?\\\",\\\"rationale\\\":\\\"Priorizacion\\\"}]\"}]}}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var live = service("live", "http://127.0.0.1:" + server.getAddress().getPort());

            var suggestions = live.suggest(JOB_TITLE, JOB_DESCRIPTION, "Pensamiento analitico", "Usa datos",
                    List.of("Pregunta existente"));

            assertThat(suggestions).extracting("statement").containsExactly("Como priorizas un analisis urgente?");
            assertThat(sentBody.get()).contains(JOB_TITLE, JOB_DESCRIPTION, "Pensamiento analitico", "Usa datos",
                    "Pregunta existente");
            assertThat(sentBody.get()).doesNotContainIgnoringCase("candidateId").doesNotContainIgnoringCase("transcript");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void thePromptCarriesOnlyThePostingTheCriterionAndTheExistingQuestions() {
        var prompt = QuestionSuggestionServiceImpl.prompt(JOB_TITLE, JOB_DESCRIPTION, CRITERION, CRITERION_DESCRIPTION,
                List.of("¿Qué es un LEFT JOIN?"));

        assertThat(prompt).contains(JOB_TITLE, JOB_DESCRIPTION, CRITERION, CRITERION_DESCRIPTION, "¿Qué es un LEFT JOIN?",
                "No pidas datos personales");
    }

    @Test
    void theSuggestionPortCannotReceiveCandidateData() {
        assertThat(Arrays.stream(QuestionSuggestionService.class.getMethods())
                .filter(method -> method.getName().equals("suggest"))
                .flatMap(method -> Arrays.stream(method.getParameterTypes())))
                .as("suggest() only takes the posting's and the criterion's text and the existing questions")
                .containsExactly(String.class, String.class, String.class, String.class, List.class);
    }
}
