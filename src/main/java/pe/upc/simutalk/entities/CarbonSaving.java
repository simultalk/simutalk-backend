package pe.upc.simutalk.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * CO2e not emitted because the candidate took the interview asynchronously instead of
 * travelling to the company: one saving per application (one avoided round trip).
 * <p>
 * Rule: {@code kgCo2eAvoided = distanceKm × emissionFactor}, computed here and rounded to
 * three decimals (HALF_UP). {@code jobPostingId} and {@code companyId} are copies used to
 * aggregate reports.
 */
@Getter
@Entity
@Table(name = "carbon_savings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CarbonSaving extends AuditableAggregateRoot<CarbonSaving> {

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /** Round-trip distance not travelled, in km. */
    @Column(name = "distance_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal distanceKm;

    /** kg CO2e per km travelled. */
    @Column(name = "emission_factor", nullable = false, precision = 8, scale = 4)
    private BigDecimal emissionFactor;

    @Column(name = "kg_co2e_avoided", nullable = false, precision = 10, scale = 3)
    private BigDecimal kgCo2eAvoided;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    private CarbonSaving(Long applicationId, Long jobPostingId, Long companyId, BigDecimal distanceKm,
                         BigDecimal emissionFactor, Instant computedAt) {
        this.applicationId = applicationId;
        this.jobPostingId = jobPostingId;
        this.companyId = companyId;
        this.distanceKm = distanceKm.setScale(2, RoundingMode.HALF_UP);
        this.emissionFactor = emissionFactor.setScale(4, RoundingMode.HALF_UP);
        this.kgCo2eAvoided = distanceKm.multiply(emissionFactor).setScale(3, RoundingMode.HALF_UP);
        this.computedAt = computedAt;
    }

    public static CarbonSaving compute(Long applicationId, Long jobPostingId, Long companyId, BigDecimal distanceKm,
                                       BigDecimal emissionFactor, Instant computedAt) {
        requirePositive(applicationId, "Application id");
        requirePositive(jobPostingId, "Job posting id");
        requirePositive(companyId, "Company id");
        if (distanceKm == null || distanceKm.signum() < 0) {
            throw new IllegalArgumentException("Distance must be zero or positive");
        }
        if (emissionFactor == null || emissionFactor.signum() <= 0) {
            throw new IllegalArgumentException("Emission factor must be positive");
        }
        if (computedAt == null) {
            throw new IllegalArgumentException("Computation instant is required");
        }
        return new CarbonSaving(applicationId, jobPostingId, companyId, distanceKm, emissionFactor, computedAt);
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be a positive number");
        }
    }
}
