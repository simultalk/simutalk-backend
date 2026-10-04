package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.services.AnswerScoringService;

import pe.upc.simutalk.dtos.CandidatePersonalData;

import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ley 29733: what leaves the platform towards the AI provider must never contain the candidate's
 * name, document, age, district or id. Checked on the payload the adapter actually builds and on
 * the HTTP body it actually sends.
 */
class AnswerScoringPrivacyTest {

    private static final long CANDIDATE_ID = 48213L;
    private static final CandidatePersonalData ROSA = new CandidatePersonalData(CANDIDATE_ID, "Rosa", "Quispe Mamani",
            "45879632", "+51987654321", "San Juan de Lurigancho", LocalDate.of(1996, 3, 14), "rquispe.contacto@gmail.com");
    private static final String TRANSCRIPT = "Buenas tardes, soy Rosa Quispe Mamani, con DNI 45879632, tengo 30 años y vivo "
            + "en San Juan de Lurigancho. Para limpiar la tabla primero contaría los clientes duplicados por documento "
            + "y normalizaría las fechas a formato ISO. Pueden escribirme a rosa.quispe@correo.pe o llamarme al 987654321. "
            + "Mi correo personal es RQuispe.Contacto@gmail.com.";
    private static final List<String> FORBIDDEN = List.of("Rosa", "Quispe", "Mamani", "45879632", "30 años",
            "San Juan de Lurigancho", "Lurigancho", "1996", "rosa.quispe@correo.pe", "987654321", String.valueOf(CANDIDATE_ID));

    private final TranscriptAnonymizer anonymizer = new TranscriptAnonymizer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void builtPayloadContainsOnlyTheAnonymizedTranscriptAndTheCriterion() throws Exception {
        var anonymized = anonymizer.anonymize(TRANSCRIPT, ROSA).text();

        var payload = GeminiRequestFactory.build(anonymized, "Pensamiento analítico",
                "Descompone problemas de negocio y los resuelve con datos.");
        var json = objectMapper.writeValueAsString(payload);

        assertThat(payload).containsOnlyKeys("contents", "generationConfig");
        FORBIDDEN.forEach(value -> assertThat(json).as("payload must not contain '%s'", value).doesNotContain(value));
        assertThat(json).as("the profile e-mail never reaches the AI provider").doesNotContainIgnoringCase(ROSA.email());
        assertThat(json).contains("Pensamiento analítico", "[NOMBRE]", "[DOCUMENTO]", "[EDAD]", "[DIRECCION]",
                "contaría los clientes duplicados");
    }

    @Test
    void httpBodySentToGeminiHasNoPersonalData() throws Exception {
        var sentBody = new AtomicReference<String>();
        var sentPath = new AtomicReference<String>();
        var sentApiKey = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            sentPath.set(exchange.getRequestURI().toString());
            sentApiKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            sentBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            var response = ("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                    + "\"{\\\"score\\\":8.1,\\\"confidence\\\":0.9,\\\"excerpt\\\":\\\"normalizaría las fechas a formato ISO\\\",\\\"aiGeneratedSuspicion\\\":false}\"}]}}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            var service = new AnswerScoringServiceImpl("live", baseUrl, "gemini-test", "test-key", Duration.ofSeconds(5),
                    1, Duration.ofMillis(10), 10, RestClient.builder(), objectMapper);

            var anonymized = anonymizer.anonymize(TRANSCRIPT, ROSA).text();
            var result = service.score(anonymized, "Pensamiento analítico", "Resuelve problemas con datos.");

            assertThat(sentPath.get()).isEqualTo("/v1beta/models/gemini-test:generateContent");
            assertThat(sentApiKey.get()).isEqualTo("test-key");
            assertThat(sentBody.get()).isNotBlank().contains("contaría los clientes duplicados");
            FORBIDDEN.forEach(value -> assertThat(sentBody.get()).as("HTTP body must not contain '%s'", value).doesNotContain(value));
            assertThat(sentBody.get()).as("the profile e-mail never reaches the AI provider").doesNotContainIgnoringCase(ROSA.email());
            assertThat(result.isAvailable()).isTrue();
            assertThat(anonymized.substring(result.startOffset(), result.endOffset()))
                    .isEqualTo("normalizaría las fechas a formato ISO");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void theScoringPortCannotReceiveCandidateIdentifiers() {
        assertThat(Arrays.stream(AnswerScoringService.class.getMethods())
                .filter(method -> method.getName().equals("score"))
                .flatMap(method -> Arrays.stream(method.getParameterTypes())))
                .as("score() only takes the anonymized transcript, the criterion name and its description")
                .containsExactly(String.class, String.class, String.class);
    }
}
