package pe.upc.simutalk.dtos;

/**
 * What assessment knows about the interview session it scores (taken from interviews' ACL).
 *
 * @param status InterviewSessionStatus name, e.g. COMPLETED
 */
public record InterviewSessionSnapshot(Long interviewSessionId, Long applicationId, Long jobPostingId, Long candidateId,
                                       String status) {

    public static final String COMPLETED = "COMPLETED";

    public boolean isCompleted() {
        return COMPLETED.equals(status);
    }
}
