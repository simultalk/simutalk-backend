package pe.upc.simutalk.services;

import pe.upc.simutalk.entities.Assessment;
import pe.upc.simutalk.enums.CriterionKind;
import pe.upc.simutalk.enums.RankingBadge;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Pure rules of the explainable ranking: ordering, anonymization and badges.
 */
public final class RankingPolicy {

    public static final BigDecimal STRONG_EVIDENCE_CONFIDENCE = new BigDecimal("0.75");

    /** Highest weighted score first; ties go to whoever was assessed first. */
    public static final Comparator<Assessment> ORDER = Comparator
            .comparing(Assessment::getWeightedScore).reversed()
            .thenComparing(Assessment::getComputedAt)
            .thenComparing(Assessment::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private RankingPolicy() {
    }

    /**
     * When the job posting uses anonymized screening, anonymization is forced and the request
     * cannot turn it off.
     */
    public static boolean isAnonymized(boolean requested, boolean forcedByJobPosting) {
        return forcedByJobPosting || requested;
    }

    /**
     * @param mandatoryCertificationCriterionIds CERTIFICATION criteria marked mandatory
     */
    public static List<RankingBadge> badgesFor(Assessment assessment, int rank, long verifiedCertifications,
                                               Set<Long> mandatoryCertificationCriterionIds) {
        var badges = new ArrayList<RankingBadge>();
        if (rank == 1) {
            badges.add(RankingBadge.TOP_RANKED);
        }
        var competencies = assessment.getCriterionScores().stream()
                .filter(score -> score.getCriterionKind() == CriterionKind.COMPETENCY)
                .toList();
        if (!competencies.isEmpty() && competencies.stream()
                .allMatch(score -> score.getConfidence().compareTo(STRONG_EVIDENCE_CONFIDENCE) >= 0)) {
            badges.add(RankingBadge.STRONG_EVIDENCE);
        }
        if (verifiedCertifications > 0) {
            badges.add(RankingBadge.VERIFIED_CERTIFICATIONS);
        }
        var missesMandatory = assessment.getCriterionScores().stream()
                .anyMatch(score -> mandatoryCertificationCriterionIds.contains(score.getCriterionId())
                        && score.getScore().signum() == 0);
        if (missesMandatory) {
            badges.add(RankingBadge.MISSING_MANDATORY_CERTIFICATION);
        }
        if (!assessment.getIntegrityFlags().isEmpty()) {
            badges.add(RankingBadge.INTEGRITY_ALERT);
        }
        return badges;
    }
}
