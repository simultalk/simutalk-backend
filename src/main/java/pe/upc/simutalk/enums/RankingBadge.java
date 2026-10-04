package pe.upc.simutalk.enums;

/**
 * Explainable badges shown next to a candidate in the ranking.
 */
public enum RankingBadge {
    /** First place of the ranking. */
    TOP_RANKED,
    /** Every COMPETENCY score has confidence of at least 0.75. */
    STRONG_EVIDENCE,
    /** Holds at least one verified, non-expired certification. */
    VERIFIED_CERTIFICATIONS,
    /** A mandatory CERTIFICATION criterion scored 0. */
    MISSING_MANDATORY_CERTIFICATION,
    /** The assessment raised integrity flags that a human should review. */
    INTEGRITY_ALERT
}
