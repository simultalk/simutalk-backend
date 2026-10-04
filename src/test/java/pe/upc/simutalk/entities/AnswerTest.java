package pe.upc.simutalk.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankTranscript(String transcript) {
        assertThatThrownBy(() -> new Answer(1L, transcript, null, 60, Instant.now(), false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transcript");
    }

    @Test
    void followUpRequiresParentAnswer() {
        assertThatThrownBy(() -> new Answer(1L, "Revisaría primero los filtros de fecha.", null, 45, Instant.now(), true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parent");
    }

    @Test
    void regularAnswerCannotHaveParent() {
        assertThatThrownBy(() -> new Answer(1L, "Usaría un LEFT JOIN.", null, 45, Instant.now(), false, 3L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveDuration() {
        assertThatThrownBy(() -> new Answer(1L, "Usaría un LEFT JOIN.", null, 0, Instant.now(), false, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createsFollowUpWithParent() {
        var answer = new Answer(1L, "  Revisaría primero los filtros de fecha. ", " ", 45, Instant.now(), true, 3L);

        assertThat(answer.getTranscript()).isEqualTo("Revisaría primero los filtros de fecha.");
        assertThat(answer.getAudioUrl()).isNull();
        assertThat(answer.isFollowUp()).isTrue();
        assertThat(answer.getParentAnswerId()).isEqualTo(3L);
    }
}
