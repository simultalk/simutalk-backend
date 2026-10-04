package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.NotificationType;

/**
 * One e-mail to a candidate about their application. Contains personal data (name and e-mail): it goes
 * to the mail provider only, never to the AI provider.
 */
public record CandidateNotification(NotificationType type, String recipientEmail, String recipientName,
                                    String jobPostingTitle) {

    public CandidateNotification {
        if (type == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient e-mail is required");
        }
        if (jobPostingTitle == null || jobPostingTitle.isBlank()) {
            throw new IllegalArgumentException("Job posting title is required");
        }
        recipientEmail = recipientEmail.strip();
        recipientName = recipientName == null ? "" : recipientName.strip();
        jobPostingTitle = jobPostingTitle.strip();
    }
}
