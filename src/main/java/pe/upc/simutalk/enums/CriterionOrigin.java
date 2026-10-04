package pe.upc.simutalk.enums;

/**
 * Who proposed an evaluation criterion. {@code AI_SUGGESTED} marks a criterion the recruiter accepted
 * from the automatic suggestions; the recruiter always sets its weight.
 */
public enum CriterionOrigin {
    AI_SUGGESTED,
    MANUAL
}
