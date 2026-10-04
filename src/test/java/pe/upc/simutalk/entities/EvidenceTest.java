package pe.upc.simutalk.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvidenceTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void excerptCannotBeEmpty(String excerpt) {
        assertThatThrownBy(() -> new Evidence(1L, excerpt, 0, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({"10, 10", "12, 3", "-1, 5"})
    void offsetsMustBeCoherent(int start, int end) {
        assertThatThrownBy(() -> new Evidence(1L, "fragmento", start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("start < end");
    }

    @Test
    void createsValidEvidence() {
        var evidence = new Evidence(1L, "Usaría un LEFT JOIN", 4, 23);

        assertThat(evidence.getEndOffset() - evidence.getStartOffset()).isEqualTo(19);
    }
}
