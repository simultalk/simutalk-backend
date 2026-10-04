package pe.upc.simutalk.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeightTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 50, 100})
    void acceptsValuesBetweenOneAndOneHundred(int value) {
        assertThat(new Weight(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 0, 101})
    void rejectsValuesOutOfRange(int value) {
        assertThatThrownBy(() -> new Weight(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new Weight(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
