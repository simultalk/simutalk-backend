package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.GeoPoint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CommuteDistancePolicyTest {

    @Test
    void oneDegreeOfLatitudeIsAbout111Km() {
        assertThat(CommuteDistancePolicy.haversineKm(new GeoPoint(0, 0), new GeoPoint(1, 0))).isCloseTo(111.19, within(0.1));
    }

    @Test
    void roundTripAppliesRoadFactorAndDoublesTheDistance() {
        var from = new GeoPoint(0, 0);
        var to = new GeoPoint(0.1, 0);
        var straight = CommuteDistancePolicy.haversineKm(from, to);

        var roundTrip = CommuteDistancePolicy.roundTripKm(from, to, 1.3);

        assertThat(roundTrip.doubleValue()).isCloseTo(straight * 1.3 * 2, within(0.01));
    }

    @Test
    void sameDistrictMeansNoTrip() {
        var sanIsidro = new GeoPoint(-12.097, -77.0365);

        assertThat(CommuteDistancePolicy.roundTripKm(sanIsidro, sanIsidro, 1.3)).isEqualByComparingTo("0");
    }

    @Test
    void rejectsRoadFactorBelowOne() {
        assertThatThrownBy(() -> CommuteDistancePolicy.roundTripKm(new GeoPoint(0, 0), new GeoPoint(1, 1), 0.9))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
