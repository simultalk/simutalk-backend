package pe.upc.simutalk.enums;

import java.util.Optional;

/** E-mails sent to a candidate when their application moves to a stage they must hear about. */
public enum NotificationType {
    /** The application reached INTERVIEWING: an interview session was created for it. */
    INTERVIEW_INVITATION,
    /** The application reached SHORTLISTED, the final shortlist. */
    SHORTLISTED,
    /** The application was REJECTED. */
    REJECTED;

    /** The notification a stage triggers, if any. */
    public static Optional<NotificationType> forStatus(ApplicationStatus status) {
        if (status == null) {
            return Optional.empty();
        }
        return switch (status) {
            case INTERVIEWING -> Optional.of(INTERVIEW_INVITATION);
            case SHORTLISTED -> Optional.of(SHORTLISTED);
            case REJECTED -> Optional.of(REJECTED);
            default -> Optional.empty();
        };
    }
}
