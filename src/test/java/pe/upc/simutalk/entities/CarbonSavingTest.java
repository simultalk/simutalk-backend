package pe.upc.simutalk.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarbonSavingTest {

    @Test
    void kgAvoidedIsDistanceTimesFactorRoundedToThreeDecimals() {
        var saving = CarbonSaving.compute(1L, 2L, 3L, new BigDecimal("27.35"), new BigDecimal("0.1234"), Instant.now());

        // 27.35 * 0.1234 = 3.37499 -> 3.375
        assertThat(saving.getKgCo2eAvoided()).isEqualByComparingTo("3.375");
        assertThat(saving.getKgCo2eAvoided().scale()).isEqualTo(3);
    }

    @Test
    void zeroDistanceSavesNothing() {
        var saving = CarbonSaving.compute(1L, 2L, 3L, BigDecimal.ZERO, new BigDecimal("0.12"), Instant.now());

        assertThat(saving.getKgCo2eAvoided()).isEqualByComparingTo("0");
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> CarbonSaving.compute(1L, 2L, 3L, new BigDecimal("-1"), new BigDecimal("0.12"), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CarbonSaving.compute(1L, 2L, 3L, BigDecimal.TEN, BigDecimal.ZERO, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CarbonSaving.compute(null, 2L, 3L, BigDecimal.TEN, BigDecimal.ONE, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theFactorKeepsItsThreeDecimalsAndOnlyTheResultIsRounded() {
        var factor = new BigDecimal("0.121");

        var saving = CarbonSaving.compute(1L, 2L, 3L, new BigDecimal("81.23"), factor, Instant.now());

        // 81.23 * 0.121 = 9.82883 -> 9.829; a factor cut to two decimals (0.12) would give 9.748
        assertThat(saving.getKgCo2eAvoided()).isEqualByComparingTo("9.829");
        assertThat(saving.getEmissionFactor()).isEqualByComparingTo("0.121");
    }

    @Test
    void theDemoTripsAddUpWithTheFullFactor() {
        var factor = new BigDecimal("0.121");

        var first = CarbonSaving.compute(1L, 2L, 3L, new BigDecimal("35.36"), factor, Instant.now());
        var second = CarbonSaving.compute(4L, 2L, 3L, new BigDecimal("45.87"), factor, Instant.now());

        // 35.36 * 0.121 = 4.27856 -> 4.279 ; 45.87 * 0.121 = 5.55027 -> 5.550
        assertThat(first.getKgCo2eAvoided()).isEqualByComparingTo("4.279");
        assertThat(second.getKgCo2eAvoided()).isEqualByComparingTo("5.550");
        assertThat(first.getKgCo2eAvoided().add(second.getKgCo2eAvoided())).isEqualByComparingTo("9.829");
    }
}
