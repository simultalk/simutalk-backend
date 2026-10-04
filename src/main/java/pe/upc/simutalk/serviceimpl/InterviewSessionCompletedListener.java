package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.InterviewSessionCompletedEvent;
import pe.upc.simutalk.dtos.RecordCarbonSavingCommand;
import pe.upc.simutalk.services.CarbonSavingCommandService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Records the carbon saving of a completed interview once interviews' transaction has
 * committed, in a transaction of its own. A failure here is logged and never undoes the
 * interview.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionCompletedListener {

    private final CarbonSavingCommandService carbonSavingCommandService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(InterviewSessionCompletedEvent event) {
        try {
            carbonSavingCommandService.handle(
                    new RecordCarbonSavingCommand(event.applicationId(), event.jobPostingId(), event.candidateId()))
                    .ifPresent(saving -> log.info("Carbon saving recorded for application {}: {} kg CO2e",
                            saving.getApplicationId(), saving.getKgCo2eAvoided()));
        } catch (RuntimeException ex) {
            log.warn("Could not record the carbon saving of application {}: {}", event.applicationId(), ex.getMessage());
        }
    }
}
