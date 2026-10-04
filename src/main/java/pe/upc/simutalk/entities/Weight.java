package pe.upc.simutalk.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Relative weight of an evaluation criterion within a job posting.
 * Must be an integer between 1 and 100. The weights of all criteria of a
 * published job posting add up to exactly {@value #TOTAL}.
 */
@Embeddable
public record Weight(@Column(name = "weight", nullable = false) Integer value) {

    public static final int MIN = 1;
    public static final int MAX = 100;
    public static final int TOTAL = 100;

    public Weight {
        if (value == null || value < MIN || value > MAX) {
            throw new IllegalArgumentException("Weight must be an integer between %d and %d".formatted(MIN, MAX));
        }
    }
}
