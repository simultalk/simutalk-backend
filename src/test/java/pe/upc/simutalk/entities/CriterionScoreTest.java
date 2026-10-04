package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.CriterionKind;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriterionScoreTest {

    @Test
    void competencyWithoutEvidenceIsRejected() {
        assertThatThrownBy(() -> new CriterionScore(1L, "Pensamiento analítico", CriterionKind.COMPETENCY,
                new BigDecimal("8.0"), 40, new BigDecimal("0.8"), List.of()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("evidence");
    }

    @Test
    void certificationDoesNotNeedEvidence() {
        var score = new CriterionScore(2L, "Certificación", CriterionKind.CERTIFICATION, new BigDecimal("7"), 30,
                BigDecimal.ONE, null);

        assertThat(score.getEvidences()).isEmpty();
        assertThat(score.getScore()).isEqualByComparingTo("7.0");
    }

    @Test
    void scoreAndConfidenceMustBeInRange() {
        assertThatThrownBy(() -> new CriterionScore(2L, "C", CriterionKind.CERTIFICATION, new BigDecimal("10.1"), 30,
                BigDecimal.ONE, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CriterionScore(2L, "C", CriterionKind.CERTIFICATION, new BigDecimal("-0.1"), 30,
                BigDecimal.ONE, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CriterionScore(2L, "C", CriterionKind.CERTIFICATION, BigDecimal.ONE, 30,
                new BigDecimal("1.01"), null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scoreIsKeptWithOneDecimal() {
        var score = new CriterionScore(2L, "C", CriterionKind.CERTIFICATION, new BigDecimal("7.25"), 30,
                new BigDecimal("0.555"), null);

        assertThat(score.getScore()).isEqualByComparingTo("7.3");
        assertThat(score.getConfidence()).isEqualByComparingTo("0.56");
    }
}
