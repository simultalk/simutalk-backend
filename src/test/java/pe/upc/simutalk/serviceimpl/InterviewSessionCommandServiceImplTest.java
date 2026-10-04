package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CompleteInterviewSessionCommand;
import pe.upc.simutalk.dtos.CreateInterviewSessionCommand;
import pe.upc.simutalk.dtos.InterviewSessionCompletedEvent;
import pe.upc.simutalk.dtos.RecordAnswerCommand;
import pe.upc.simutalk.dtos.StartInterviewSessionCommand;
import pe.upc.simutalk.entities.InterviewSession;
import pe.upc.simutalk.entities.Question;
import pe.upc.simutalk.enums.InterviewSessionStatus;
import pe.upc.simutalk.enums.QuestionOrigin;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.repositories.InterviewSessionRepository;
import pe.upc.simutalk.repositories.QuestionRepository;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InterviewSessionCommandServiceImplTest {

    private final InterviewSessionRepository sessions = mock(InterviewSessionRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final RecruitmentContextFacade recruitment = mock(RecruitmentContextFacade.class);
    private final org.springframework.context.ApplicationEventPublisher events = mock(org.springframework.context.ApplicationEventPublisher.class);
    private final InterviewSessionCommandServiceImpl service =
            new InterviewSessionCommandServiceImpl(sessions, questions, new ExternalRecruitmentService(recruitment), events);

    @BeforeEach
    void setUp() {
        when(recruitment.fetchApplicationStatus(anyLong())).thenReturn("");
        when(recruitment.fetchApplicationStatus(50L)).thenReturn("RECEIVED");
        when(recruitment.fetchApplicationStatus(51L)).thenReturn("INTERVIEWING");
        when(recruitment.fetchJobPostingIdByApplicationId(50L)).thenReturn(1L);
        when(recruitment.fetchCandidateIdByApplicationId(50L)).thenReturn(7L);
        when(questions.countByJobPostingId(1L)).thenReturn(1L);
        when(sessions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static CreateInterviewSessionCommand create(long applicationId) {
        return new CreateInterviewSessionCommand(applicationId, LocalDate.now().plusDays(7));
    }

    @Test
    void creatingASessionMovesTheApplicationToInterviewing() {
        var session = service.handle(create(50L));

        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.PENDING);
        assertThat(session.getJobPostingId()).isEqualTo(1L);
        assertThat(session.getCandidateId()).isEqualTo(7L);
        verify(recruitment).markApplicationAsInterviewing(50L);
    }

    @Test
    void applicationMustBeReceived() {
        assertThatThrownBy(() -> service.handle(create(51L)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("RECEIVED");
        verify(recruitment, never()).markApplicationAsInterviewing(anyLong());
        verify(sessions, never()).save(any());
    }

    @Test
    void unknownApplicationIsNotFound() {
        assertThatThrownBy(() -> service.handle(create(99L))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void oneSessionPerApplication() {
        when(sessions.existsByApplicationId(50L)).thenReturn(true);

        assertThatThrownBy(() -> service.handle(create(50L))).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void jobPostingWithoutQuestionsCannotBeInterviewed() {
        when(questions.countByJobPostingId(1L)).thenReturn(0L);

        assertThatThrownBy(() -> service.handle(create(50L)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("no interview questions");
    }

    @Test
    void completingTheSessionMovesTheApplicationToAssessed() {
        var session = new InterviewSession(50L, 1L, 7L, LocalDate.now().plusDays(7), Instant.now());
        ReflectionTestUtils.setField(session, "id", 900L);
        var question = new Question(1L, 11L, "¿Qué es un LEFT JOIN?", 120, 1, QuestionOrigin.MANUAL, false);
        ReflectionTestUtils.setField(question, "id", 5L);
        when(sessions.findWithAnswersById(900L)).thenReturn(Optional.of(session));
        when(questions.findById(5L)).thenReturn(Optional.of(question));

        service.handle(new StartInterviewSessionCommand(900L));
        service.handle(new RecordAnswerCommand(900L, 5L, "Devuelve todas las filas de la tabla izquierda.", null, 60, false, null));
        var completed = service.handle(new CompleteInterviewSessionCommand(900L));

        assertThat(completed.getStatus()).isEqualTo(InterviewSessionStatus.COMPLETED);
        verify(recruitment).markApplicationAsAssessed(50L);
        verify(events).publishEvent(new InterviewSessionCompletedEvent(
                900L, 50L, 1L, 7L, completed.getFinishedAt()));
    }
}
