package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.QuestionOrigin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionTest {

    private static Question question(String statement, int maxDurationSeconds, int position) {
        return new Question(1L, 10L, statement, maxDurationSeconds, position, QuestionOrigin.MANUAL, false);
    }

    @Test
    void createsValidQuestion() {
        var question = question("  ¿Cómo limpiarías una tabla con clientes duplicados?  ", 120, 1);

        assertThat(question.getStatement()).isEqualTo("¿Cómo limpiarías una tabla con clientes duplicados?");
        assertThat(question.getPosition()).isEqualTo(1);
        assertThat(question.belongsTo(1L)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void rejectsBlankStatement(String statement) {
        assertThatThrownBy(() -> question(statement, 120, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statement");
    }

    @Test
    void rejectsStatementLongerThan500Characters() {
        assertThatThrownBy(() -> question("a".repeat(501), 120, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(question("a".repeat(500), 120, 1).getStatement()).hasSize(500);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 29, 601, 3600, -30})
    void rejectsDurationOutOfRange(int seconds) {
        assertThatThrownBy(() -> question("¿Qué es un LEFT JOIN?", seconds, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 30 and 600");
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 600})
    void acceptsDurationLimits(int seconds) {
        assertThat(question("¿Qué es un LEFT JOIN?", seconds, 1).getMaxDurationSeconds()).isEqualTo(seconds);
    }

    @Test
    void rejectsPositionBelowOne() {
        assertThatThrownBy(() -> question("¿Qué es un LEFT JOIN?", 120, 0)).isInstanceOf(IllegalArgumentException.class);
        var question = question("¿Qué es un LEFT JOIN?", 120, 3);
        assertThatThrownBy(() -> question.moveTo(-1)).isInstanceOf(IllegalArgumentException.class);
        question.moveTo(1);
        assertThat(question.getPosition()).isEqualTo(1);
    }

    @Test
    void updateKeepsTheSameRules() {
        var question = question("¿Qué es un LEFT JOIN?", 120, 1);

        assertThatThrownBy(() -> question.updateDetails(10L, " ", 120, QuestionOrigin.MANUAL, false))
                .isInstanceOf(IllegalArgumentException.class);
        question.updateDetails(11L, "¿Qué es un INNER JOIN?", 90, QuestionOrigin.AI_SUGGESTED, true);
        assertThat(question.getCriterionId()).isEqualTo(11L);
        assertThat(question.isAllowsFollowUp()).isTrue();
    }
}
