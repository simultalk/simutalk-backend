package pe.upc.simutalk.entities;

import pe.upc.simutalk.dtos.InterviewSessionSnapshot;
import pe.upc.simutalk.enums.CriterionKind;
import pe.upc.simutalk.enums.FlagSeverity;
import pe.upc.simutalk.enums.IntegrityFlagType;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssessmentTest {

    private static final InterviewSessionSnapshot COMPLETED =
            new InterviewSessionSnapshot(900L, 50L, 10L, 7L, "COMPLETED");

    private static CriterionScore competency(long criterionId, String score, int weight) {
        return new CriterionScore(criterionId, "Criterio " + criterionId, CriterionKind.COMPETENCY, new BigDecimal(score),
                weight, new BigDecimal("0.80"), List.of(new Evidence(1L, "Usaría un LEFT JOIN.", 0, 20)));
    }

    private static CriterionScore certification(long criterionId, String score, int weight) {
        return new CriterionScore(criterionId, "Certificación", CriterionKind.CERTIFICATION, new BigDecimal(score),
                weight, BigDecimal.ONE, List.of());
    }

    @Test
    void weightedScoreIsTheWeightedSumDividedBy100() {
        var assessment = Assessment.calculate(COMPLETED,
                List.of(competency(1, "8.0", 40), competency(2, "6.5", 30), certification(3, "7.0", 30)),
                List.of(), "mock-1", Instant.now());

        // (8.0*40 + 6.5*30 + 7.0*30) / 100 = (320 + 195 + 210) / 100 = 7.25 -> 7.3
        assertThat(assessment.getWeightedScore()).isEqualByComparingTo("7.3");
        assertThat(assessment.getWeightedScore().scale()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({
            // score1, score2 -> weighted with weights 50/50
            "7.0, 8.0, 7.5",
            "7.1, 8.0, 7.6",   // 7.55 rounds HALF_UP to 7.6
            "7.1, 7.9, 7.5",   // 7.50 stays 7.5
            "0.0, 0.1, 0.1",   // 0.05 rounds up to 0.1
            "10.0, 10.0, 10.0",
            "0.0, 0.0, 0.0"
    })
    void roundsToOneDecimalHalfUp(String first, String second, String expected) {
        var assessment = Assessment.calculate(COMPLETED, List.of(competency(1, first, 50), competency(2, second, 50)),
                List.of(), "mock-1", Instant.now());

        assertThat(assessment.getWeightedScore()).isEqualByComparingTo(expected);
    }

    @Test
    void roundingUsesThirdDecimalCorrectly() {
        // (9.3*33 + 4.1*33 + 5.0*34) / 100 = (306.9 + 135.3 + 170) / 100 = 6.122 -> 6.1
        var assessment = Assessment.calculate(COMPLETED,
                List.of(competency(1, "9.3", 33), competency(2, "4.1", 33), competency(3, "5.0", 34)),
                List.of(), "mock-1", Instant.now());

        assertThat(assessment.getWeightedScore()).isEqualByComparingTo("6.1");
    }

    @Test
    void cannotAssessASessionThatIsNotCompleted() {
        var inProgress = new InterviewSessionSnapshot(900L, 50L, 10L, 7L, "IN_PROGRESS");

        assertThatThrownBy(() -> Assessment.calculate(inProgress, List.of(competency(1, "8.0", 100)), List.of(),
                "mock-1", Instant.now()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void weightsMustAddUpTo100() {
        assertThatThrownBy(() -> Assessment.calculate(COMPLETED, List.of(competency(1, "8.0", 60), competency(2, "8.0", 30)),
                List.of(), "mock-1", Instant.now()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("90");
    }

    @Test
    void criterionCannotBeScoredTwice() {
        assertThatThrownBy(() -> Assessment.calculate(COMPLETED, List.of(competency(1, "8.0", 50), competency(1, "7.0", 50)),
                List.of(), "mock-1", Instant.now()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void keepsSessionIdsFlagsAndEvidenceCount() {
        var flag = new IntegrityFlag(IntegrityFlagType.CV_INCONSISTENCY, FlagSeverity.MEDIUM, "1 certificación rechazada",
                Instant.now());
        var assessment = Assessment.calculate(COMPLETED, List.of(competency(1, "8.0", 70), certification(2, "0.0", 30)),
                List.of(flag), "mock-1", Instant.now());

        assertThat(assessment.getInterviewSessionId()).isEqualTo(900L);
        assertThat(assessment.getJobPostingId()).isEqualTo(10L);
        assertThat(assessment.getCandidateId()).isEqualTo(7L);
        assertThat(assessment.getIntegrityFlags()).hasSize(1);
        assertThat(assessment.countEvidences()).isEqualTo(1);
    }

    @Test
    void recordsTheCandidateFeedbackWithinItsLimits() {
        var assessment = Assessment.calculate(COMPLETED, List.of(competency(1, "8.0", 100)), List.of(), "mock-1", Instant.now());
        assertThat(assessment.getFeedbackSummary()).isNull();

        assessment.recordFeedback("  Tu puntaje ponderado fue 8 de 10.  ");

        assertThat(assessment.getFeedbackSummary()).isEqualTo("Tu puntaje ponderado fue 8 de 10.");
        assertThatThrownBy(() -> assessment.recordFeedback(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> assessment.recordFeedback("x".repeat(Assessment.FEEDBACK_MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
