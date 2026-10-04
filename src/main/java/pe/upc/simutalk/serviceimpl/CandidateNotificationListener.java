package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.ApplicationStatusChangedEvent;
import pe.upc.simutalk.dtos.CandidateNotification;
import pe.upc.simutalk.entities.JobPosting;
import pe.upc.simutalk.enums.NotificationType;
import pe.upc.simutalk.repositories.JobPostingRepository;
import pe.upc.simutalk.services.NotificationService;
import pe.upc.simutalk.services.ProfilesContextFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * US-24: e-mails the candidate when their application reaches INTERVIEWING (interview invitation),
 * SHORTLISTED (final shortlist) or REJECTED. Runs after the change commits and never throws: a missing
 * e-mail or a failed delivery is logged as WARN and the operation that moved the application stands.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateNotificationListener {

    private final NotificationService notificationService;
    private final ProfilesContextFacade profilesContextFacade;
    private final JobPostingRepository jobPostingRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ApplicationStatusChangedEvent event) {
        var type = NotificationType.forStatus(event.status());
        if (type.isEmpty()) {
            return;
        }
        try {
            var contact = profilesContextFacade.fetchCandidateContact(event.candidateId());
            if (contact == null || !contact.hasEmail()) {
                log.warn("{} notification for application {} not sent: the candidate has no e-mail",
                        type.get(), event.applicationId());
                return;
            }
            var title = jobPostingRepository.findById(event.jobPostingId()).map(JobPosting::getTitle).orElse("tu postulación");
            notificationService.send(new CandidateNotification(type.get(), contact.email(), contact.firstName(), title));
        } catch (RuntimeException ex) {
            log.warn("{} notification for application {} could not be sent: {}", type.get(), event.applicationId(),
                    ex.toString());
        }
    }
}
