package pe.upc.simutalk.serviceimpl;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CriterionSuggestionServiceImplTest {

    private static final String DESCRIPTION = "Análisis de datos comerciales con SQL, Excel y Python; elaboración de "
            + "reportes para clientes y presentación de hallazgos al equipo comercial.";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CriterionSuggestionServiceImpl service(String mode) {
        // mock mode: the base URL points nowhere, so any network call would fail the test
        var client = new GenerativeAiClient(mode, "http://127.0.0.1:1", "gemini-test", mode.equals("live") ? "k" : "",
                Duration.ofMillis(200), 0, Duration.ofMillis(1), 10, RestClient.builder());
        return new CriterionSuggestionServiceImpl(client, objectMapper);
    }

    @Test
    void mockModeReturnsFourCriteriaDerivedFromTheKeywordsOfTheDescription() {
        var suggestions = service("mock").suggest("Analista de Datos Junior", DESCRIPTION, List.of());

        assertThat(suggestions).hasSize(4);
        assertThat(suggestions.getFirst().name()).isEqualTo("Pensamiento analítico");
        assertThat(suggestions.getFirst().rationale()).contains("datos", "sql", "excel", "python");
        assertThat(suggestions).extracting("name").contains("Comunicación efectiva");
    }

    @Test
    void mockModeIsDeterministicAndSkipsCriteriaThePostingAlreadyHas() {
        var mock = service("mock");

        var first = mock.suggest("Analista de Datos", DESCRIPTION, List.of("pensamiento ANALÍTICO"));
        var second = mock.suggest("Analista de Datos", DESCRIPTION, List.of("pensamiento ANALÍTICO"));

        assertThat(first).isEqualTo(second).hasSize(4);
        assertThat(first).extracting("name").doesNotContain("Pensamiento analítico");
    }

    @Test
    void mockModeCompletesWithGeneralCompetenciesWhenTheTextSaysLittle() {
        var suggestions = service("mock").suggest("Puesto", "Sin detalles.", List.of());

        assertThat(suggestions).hasSize(4);
        assertThat(suggestions).allSatisfy(suggestion ->
                assertThat(suggestion.rationale()).isEqualTo("Competencia general recomendada para cualquier puesto"));
    }

    @Test
    void liveAnswersAreValidatedAgainstTheSchemaAndTheirWeightsIgnored() {
        var answer = """
                [{"name":"Pensamiento analítico","description":"Usa datos","rationale":"SQL","weight":40},
                 {"name":"pensamiento analítico","description":"repetido"},
                 {"name":"Comunicación efectiva","description":"Ya existe en la vacante"},
                 {"name":"","description":"sin nombre"},
                 {"name":"Trabajo en equipo","description":"Colabora","weight":60}]""";

        var suggestions = service("live").parse(answer, List.of("Comunicación efectiva"));

        assertThat(suggestions).extracting("name").containsExactly("Pensamiento analítico", "Trabajo en equipo");
    }

    @Test
    void liveModeFallsBackToKeywordSuggestionsWhenTheProviderFails() {
        var suggestions = service("live").suggest("Analista de Datos Junior", DESCRIPTION, List.of());

        assertThat(suggestions).hasSize(4);
        assertThat(suggestions.getFirst().name()).isEqualTo("Pensamiento analítico");
    }

    @Test
    void thePromptCarriesOnlyThePostingTextAndForbidsWeights() {
        var prompt = CriterionSuggestionServiceImpl.prompt("Analista", DESCRIPTION, List.of("Liderazgo"));

        assertThat(prompt).contains("Analista", DESCRIPTION, "Liderazgo", "No asignes pesos");
    }
}
