package pe.upc.simutalk.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Stages of an application in the recruiter's pipeline. Transitions are directed:
 * <pre>
 * RECEIVED     -> INTERVIEWING | REJECTED
 * INTERVIEWING -> ASSESSED     | REJECTED
 * ASSESSED     -> SHORTLISTED  | REJECTED
 * SHORTLISTED  -> HIRED        | REJECTED
 * REJECTED, HIRED: final
 * </pre>
 */
public enum ApplicationStatus {
    RECEIVED,
    INTERVIEWING,
    ASSESSED,
    SHORTLISTED,
    REJECTED,
    HIRED;

    public Set<ApplicationStatus> allowedNextStatuses() {
        return switch (this) {
            case RECEIVED -> EnumSet.of(INTERVIEWING, REJECTED);
            case INTERVIEWING -> EnumSet.of(ASSESSED, REJECTED);
            case ASSESSED -> EnumSet.of(SHORTLISTED, REJECTED);
            case SHORTLISTED -> EnumSet.of(HIRED, REJECTED);
            case REJECTED, HIRED -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canTransitionTo(ApplicationStatus target) {
        return target != null && allowedNextStatuses().contains(target);
    }

    public boolean isFinal() {
        return allowedNextStatuses().isEmpty();
    }
}
