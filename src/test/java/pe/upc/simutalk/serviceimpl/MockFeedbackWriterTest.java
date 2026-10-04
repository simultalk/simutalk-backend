package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.services.AnswerScoringService.CriterionFeedback;

import pe.upc.simutalk.enums.CriterionKind;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockFeedbackWriterTest {

    private static final List<CriterionFeedback> CRITERIA = List.of(
            new CriterionFeedback("Pensamiento analítico", CriterionKind.COMPETENCY, new BigDecimal("8.1"), 40, "separaría la caída"),
            new CriterionFeedback("Comunicación efectiva", CriterionKind.COMPETENCY, new BigDecimal("6.5"), 30, "empezaría por la conclusión"),
            new CriterionFeedback("Certificación en análisis de datos", CriterionKind.CERTIFICATION, new BigDecimal("0.0"), 30, null));

    @Test
    void pointsOutWhatSustainedTheScoreAndWherePointsWereLost() {
        var feedback = MockFeedbackWriter.write(new BigDecimal("5.2"), CRITERIA);

        assertThat(feedback).startsWith("Tu puntaje ponderado fue 5,2 de 10.")
                .contains("Lo que más sostuvo tu puntaje fue «Pensamiento analítico» (8,1 de 10)")
                .contains("Donde más puntos se perdieron fue «Certificación en análisis de datos» (0 de 10, peso 30 %)")
                .contains("suma 0,3 a tu puntaje ponderado");
    }

    @Test
    void neverTalksAboutRankingsOtherCandidatesOrIntegrity() {
        var feedback = MockFeedbackWriter.write(new BigDecimal("5.2"), CRITERIA).toLowerCase();

        assertThat(feedback).doesNotContain("ranking", "posición", "puesto número", "otros postulantes", "integridad", "sospech");
    }

    @Test
    void theAdapterUsesItInMockModeAndAsTheLiveFallback() {
        var objectMapper = new ObjectMapper();
        var mock = new AnswerScoringServiceImpl("mock", "http://127.0.0.1:1", "gemini-test", "", Duration.ofMillis(200),
                0, Duration.ofMillis(1), 10, RestClient.builder(), objectMapper);
        var liveWithoutProvider = new AnswerScoringServiceImpl("live", "http://127.0.0.1:1", "gemini-test", "k",
                Duration.ofMillis(200), 0, Duration.ofMillis(1), 10, RestClient.builder(), objectMapper);

        var expected = MockFeedbackWriter.write(new BigDecimal("5.2"), CRITERIA);

        assertThat(mock.summarizeFeedback(new BigDecimal("5.2"), CRITERIA)).isEqualTo(expected);
        assertThat(liveWithoutProvider.summarizeFeedback(new BigDecimal("5.2"), CRITERIA)).isEqualTo(expected);
    }

    @Test
    void theFeedbackPromptCarriesNoRankingAndOnlyAnonymizedExcerpts() {
        var prompt = GeminiRequestFactory.feedbackPrompt("5.2", CRITERIA);

        assertThat(prompt).contains("Pensamiento analítico", "separaría la caída", "No menciones posiciones, rankings");
    }
}
