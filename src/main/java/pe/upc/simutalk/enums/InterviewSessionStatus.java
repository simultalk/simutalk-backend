package pe.upc.simutalk.enums;

/**
 * PENDING -> IN_PROGRESS -> COMPLETED; PENDING or IN_PROGRESS -> EXPIRED once past due.
 * COMPLETED and EXPIRED are final.
 */
public enum InterviewSessionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    EXPIRED;

    public boolean isFinal() {
        return this == COMPLETED || this == EXPIRED;
    }
}
