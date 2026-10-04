package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.ApplicationStatusChangedEvent;
import pe.upc.simutalk.dtos.CandidateContact;
import pe.upc.simutalk.dtos.CandidateNotification;
import pe.upc.simutalk.dtos.CreateJobPostingCommand;
import pe.upc.simutalk.entities.JobPosting;
import pe.upc.simutalk.enums.ApplicationStatus;
import pe.upc.simutalk.enums.NotificationType;
import pe.upc.simutalk.repositories.JobPostingRepository;
import pe.upc.simutalk.services.NotificationService;
import pe.upc.simutalk.services.ProfilesContextFacade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CandidateNotificationListenerTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final ProfilesContextFacade profiles = mock(ProfilesContextFacade.class);
    private final JobPostingRepository postings = mock(JobPostingRepository.class);
    private final CandidateNotificationListener handler =
            new CandidateNotificationListener(notificationService, profiles, postings);

    @BeforeEach
    void setUp() {
        when(profiles.fetchCandidateContact(7L)).thenReturn(new CandidateContact(7L, "Rosa", "Rosa Quispe", "rosa@example.com"));
        when(postings.findById(100L)).thenReturn(Optional.of(
                new JobPosting(new CreateJobPostingCommand("Analista de Datos", "SQL", 1L, LocalDate.now().plusDays(10), false))));
    }

    private static ApplicationStatusChangedEvent movedTo(ApplicationStatus status) {
        return new ApplicationStatusChangedEvent(55L, 100L, 7L, status);
    }

    @Test
    void sendsTheInvitationTheShortlistAndTheRejectionMails() {
        handler.on(movedTo(ApplicationStatus.INTERVIEWING));
        handler.on(movedTo(ApplicationStatus.SHORTLISTED));
        handler.on(movedTo(ApplicationStatus.REJECTED));

        var sent = ArgumentCaptor.forClass(CandidateNotification.class);
        verify(notificationService, times(3)).send(sent.capture());
        assertThat(sent.getAllValues()).extracting(CandidateNotification::type).containsExactly(
                NotificationType.INTERVIEW_INVITATION, NotificationType.SHORTLISTED, NotificationType.REJECTED);
        assertThat(sent.getValue()).isEqualTo(new CandidateNotification(NotificationType.REJECTED, "rosa@example.com",
                "Rosa", "Analista de Datos"));
    }

    @Test
    void otherStagesSendNothing() {
        handler.on(movedTo(ApplicationStatus.ASSESSED));
        handler.on(movedTo(ApplicationStatus.HIRED));

        verifyNoInteractions(notificationService, profiles);
    }

    @Test
    void aCandidateWithoutEmailIsSkipped() {
        when(profiles.fetchCandidateContact(7L)).thenReturn(new CandidateContact(7L, "Rosa", "Rosa Quispe", null));

        handler.on(movedTo(ApplicationStatus.SHORTLISTED));

        verifyNoInteractions(notificationService);
    }

    @Test
    void aFailedDeliveryNeverInterruptsTheOperation() {
        doThrow(new IllegalStateException("provider down")).when(notificationService).send(any());
        when(profiles.fetchCandidateContact(8L)).thenThrow(new IllegalStateException("profiles down"));

        assertThatCode(() -> handler.on(movedTo(ApplicationStatus.REJECTED))).doesNotThrowAnyException();
        assertThatCode(() -> handler.on(new ApplicationStatusChangedEvent(56L, 100L, 8L, ApplicationStatus.REJECTED)))
                .doesNotThrowAnyException();
    }
}
