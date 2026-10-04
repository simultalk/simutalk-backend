package pe.upc.simutalk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@code sustainability.*} settings used to estimate avoided commute emissions.
 *
 * @param emissionFactorKgCo2ePerKm kg CO2e per km travelled
 * @param roadFactor                street distance / straight-line distance
 * @param defaultOneWayDistanceKm   used when a district has no coordinates
 * @param districts                 approximate district centroids
 */
@ConfigurationProperties(prefix = "sustainability")
public record SustainabilityProperties(BigDecimal emissionFactorKgCo2ePerKm, double roadFactor,
                                       double defaultOneWayDistanceKm, List<District> districts) {

    public SustainabilityProperties {
        districts = districts == null ? List.of() : List.copyOf(districts);
        if (emissionFactorKgCo2ePerKm == null || emissionFactorKgCo2ePerKm.signum() <= 0) {
            throw new IllegalStateException("sustainability.emission-factor-kg-co2e-per-km must be positive");
        }
        if (roadFactor < 1.0) {
            throw new IllegalStateException("sustainability.road-factor must be at least 1");
        }
    }

    public record District(String name, double latitude, double longitude) {
    }

    /** Case- and accent-insensitive lookup. */
    public Optional<District> findDistrict(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        var key = fold(name);
        return districts.stream().filter(district -> fold(district.name()).equals(key)).findFirst();
    }

    private static String fold(String value) {
        return Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
