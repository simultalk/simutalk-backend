package pe.upc.simutalk.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Reference by identity to the company that owns a job posting.
 * The company aggregate lives in another bounded context; only its id is kept here.
 */
@Embeddable
public record CompanyId(@Column(name = "company_id", nullable = false) Long value) {

    public CompanyId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Company id must be a positive number");
        }
    }
}
