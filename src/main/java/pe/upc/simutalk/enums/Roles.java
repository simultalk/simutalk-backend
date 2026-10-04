package pe.upc.simutalk.enums;

/**
 * Roles of SimuTalk.
 * <ul>
 *   <li>{@code ROLE_ADMIN}: platform support.</li>
 *   <li>{@code ROLE_RECRUITER}: company user who publishes job postings and defines criteria.</li>
 *   <li>{@code ROLE_CANDIDATE}: applicant who takes the asynchronous interview.</li>
 * </ul>
 */
public enum Roles {
    ROLE_ADMIN,
    ROLE_RECRUITER,
    ROLE_CANDIDATE;

    /**
     * Whether a user may pick this role for themselves when signing up publicly.
     */
    public boolean isSelfAssignable() {
        return this != ROLE_ADMIN;
    }
}
