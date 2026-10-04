package pe.upc.simutalk.enums;

/**
 * How a criterion is scored: COMPETENCY from interview answers (needs evidence), CERTIFICATION
 * from the candidate's verified certifications (no textual evidence).
 */
public enum CriterionKind {
    COMPETENCY,
    CERTIFICATION;

    public static CriterionKind fromName(String name) {
        try {
            return CriterionKind.valueOf(name);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unknown criterion type: " + name);
        }
    }
}
