package pe.upc.simutalk.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Peruvian taxpayer id (RUC): 11 digits starting with 10 (natural person with
 * business) or 20 (legal entity).
 */
@Embeddable
public record Ruc(@Column(name = "ruc", nullable = false, unique = true, length = 11) String value) {

    public Ruc {
        if (value == null || !value.strip().matches("(10|20)\\d{9}")) {
            throw new IllegalArgumentException("RUC must have 11 digits and start with 10 or 20");
        }
        value = value.strip();
    }
}
