package pe.upc.simutalk.services;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pe.upc.simutalk.entities.Answer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommunicationAnalyzerTest {

    private static Answer answer(long id, String transcript, int seconds) {
        var answer = new Answer(5L, transcript, null, seconds, Instant.now(), false, null);
        ReflectionTestUtils.setField(answer, "id", id);
        return answer;
    }

    @Test
    void countsFillerWordsButNotTheSameWordsUsedNormally() {
        var analysis = CommunicationAnalyzer.analyze(1L, List.of(answer(10L,
                "Eh, bueno, este... o sea, en este proyecto revisé los datos. Mmm, digamos que salió bien.", 60)));

        assertThat(analysis.fillerWordCount()).isEqualTo(6);
        assertThat(analysis.fillerWords()).extracting("word")
                .containsExactlyInAnyOrder("eh", "bueno,", "este…", "o sea", "mmm", "digamos");
        assertThat(analysis.answers().get(0).fillerWordCount()).isEqualTo(6);
    }

    @Test
    void measuresWordsPaceAndLength() {
        var analysis = CommunicationAnalyzer.analyze(1L, List.of(answer(10L, "Usaría un LEFT JOIN.", 30)));

        var answer = analysis.answers().get(0);
        assertThat(answer.words()).isEqualTo(4);
        assertThat(answer.wordsPerMinute()).isEqualTo(8);
        assertThat(answer.length()).isEqualTo("SHORT");
        assertThat(analysis.totalWords()).isEqualTo(4);
        assertThat(analysis.totalSeconds()).isEqualTo(30);
        assertThat(analysis.fillerWordsPer100Words()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void pointsToTheLiteralSentenceOfAnExampleAndOfAResult() {
        var transcript = "Me gusta analizar datos. Por ejemplo, en mi último trabajo automaticé un reporte semanal. "
                + "Con eso redujimos el tiempo de cierre en 40 %.";

        var answer = CommunicationAnalyzer.analyze(1L, List.of(answer(10L, transcript, 90))).answers().get(0);

        assertThat(answer.concreteExampleEvidence())
                .isEqualTo("Por ejemplo, en mi último trabajo automaticé un reporte semanal.");
        assertThat(answer.resultEvidence()).isEqualTo("Con eso redujimos el tiempo de cierre en 40 %.");
    }

    @Test
    void anAnswerWithoutExampleOrResultHasNoEvidence() {
        var analysis = CommunicationAnalyzer.analyze(1L, List.of(answer(10L, "Soy una persona responsable.", 20)));

        assertThat(analysis.answersWithConcreteExample()).isZero();
        assertThat(analysis.answersWithResult()).isZero();
        assertThat(analysis.answers().get(0).concreteExampleEvidence()).isNull();
        assertThat(analysis.answers().get(0).resultEvidence()).isNull();
    }

    @Test
    void understandsEnglishAnswers() {
        var analysis = CommunicationAnalyzer.analyze(1L, List.of(answer(10L,
                "Um, you know, for example in my last job I improved the onboarding. As a result we saved two days.", 40)));

        assertThat(analysis.fillerWordCount()).isEqualTo(2);
        assertThat(analysis.answersWithConcreteExample()).isEqualTo(1);
        assertThat(analysis.answersWithResult()).isEqualTo(1);
    }

    @Test
    void anInterviewWithoutAnswersGivesZeros() {
        var analysis = CommunicationAnalyzer.analyze(1L, List.of());

        assertThat(analysis.answerCount()).isZero();
        assertThat(analysis.totalWords()).isZero();
        assertThat(analysis.wordsPerMinute()).isNull();
        assertThat(analysis.fillerWords()).isEmpty();
    }
}
