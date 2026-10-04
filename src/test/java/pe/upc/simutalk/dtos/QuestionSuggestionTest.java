package pe.upc.simutalk.dtos;

import pe.upc.simutalk.entities.Question;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionSuggestionTest {

    @Test
    void stripsTheStatementAndDefaultsTheRationale() {
        var suggestion = new QuestionSuggestion("  ¿Cómo validas un reporte?  ", null);

        assertThat(suggestion.statement()).isEqualTo("¿Cómo validas un reporte?");
        assertThat(suggestion.rationale()).isEmpty();
    }

    @Test
    void aSuggestionAlwaysFitsInAScriptQuestion() {
        assertThatThrownBy(() -> new QuestionSuggestion(" ", "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuestionSuggestion(null, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuestionSuggestion("x".repeat(Question.STATEMENT_MAX_LENGTH + 1), "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new QuestionSuggestion("x".repeat(Question.STATEMENT_MAX_LENGTH), "x").statement())
                .hasSize(Question.STATEMENT_MAX_LENGTH);
    }
}
