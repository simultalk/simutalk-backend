package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CompleteInterviewSessionCommand;
import pe.upc.simutalk.dtos.CandidatePersonalData;
import pe.upc.simutalk.dtos.CreateInterviewSessionCommand;
import pe.upc.simutalk.dtos.CriterionView;
import pe.upc.simutalk.dtos.GenerateFollowUpQuestionCommand;
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
import pe.upc.simutalk.services.FollowUpQuestionService;
import pe.upc.simutalk.services.ProfilesContextFacade;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
    private final ProfilesContextFacade profiles = mock(ProfilesContextFacade.class);
    private final FollowUpQuestionService followUpQuestions = mock(FollowUpQuestionService.class);
    private final InterviewSessionCommandServiceImpl service =
            new InterviewSessionCommandServiceImpl(sessions, questions, new ExternalRecruitmentService(recruitment), events,
                    new ExternalProfilesService(profiles), new TranscriptAnonymizer(), followUpQuestions);

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

    /* ---------- US-09: follow-up question ---------- */

    /** Session 900 IN_PROGRESS with answer 300 to question 5 (criterion 11), which allows follow-ups. */
    private InterviewSession sessionWithAnswer(boolean allowsFollowUp) {
        var session = new InterviewSession(50L, 1L, 7L, LocalDate.now().plusDays(7), Instant.now());
        ReflectionTestUtils.setField(session, "id", 900L);
        var question = new Question(1L, 11L, "¿Cómo limpiarías una tabla con duplicados?", 120, 1, QuestionOrigin.MANUAL,
                allowsFollowUp);
        ReflectionTestUtils.setField(question, "id", 5L);
        when(sessions.findWithAnswersById(900L)).thenReturn(Optional.of(session));
        when(questions.findById(5L)).thenReturn(Optional.of(question));
        when(recruitment.fetchCompetencyCriterionIds(1L)).thenReturn(List.of(11L));
        when(recruitment.fetchCriteria(1L)).thenReturn(List.of(new CriterionView(11L, "Pensamiento analítico",
                "Resuelve problemas con datos", 100, "COMPETENCY", null, false)));
        when(profiles.fetchCandidatePersonalData(7L)).thenReturn(
                new CandidatePersonalData(7L, "Rosa", "Quispe", "45678912", "987654321", "Comas", null));
        service.handle(new StartInterviewSessionCommand(900L));
        service.handle(new RecordAnswerCommand(900L, 5L, "Soy Rosa y usaría un DISTINCT.", null, 60, false, null));
        ReflectionTestUtils.setField(session.getAnswers().get(0), "id", 300L);
        return session;
    }

    @Test
    void generatesTheFollowUpQuestionFromTheAnonymizedAnswerAndStoresIt() {
        var session = sessionWithAnswer(true);
        when(followUpQuestions.generate(anyString(), anyString(), anyString(), anyString())).thenReturn("¿Qué resultado obtuviste?");

        var followUp = service.handle(new GenerateFollowUpQuestionCommand(900L, 300L));

        assertThat(followUp.parentAnswerId()).isEqualTo(300L);
        assertThat(followUp.questionId()).isEqualTo(5L);
        assertThat(followUp.statement()).isEqualTo("¿Qué resultado obtuviste?");
        assertThat(session.getAnswers().get(0).getFollowUpQuestion()).isEqualTo("¿Qué resultado obtuviste?");
        verify(followUpQuestions).generate("¿Cómo limpiarías una tabla con duplicados?", "Pensamiento analítico",
                "Resuelve problemas con datos", "Soy [NOMBRE] y usaría un DISTINCT.");
    }

    @Test
    void askingAgainReturnsTheSameFollowUpQuestionWithoutCallingTheProvider() {
        sessionWithAnswer(true);
        when(followUpQuestions.generate(anyString(), anyString(), anyString(), anyString())).thenReturn("¿Qué resultado obtuviste?");

        service.handle(new GenerateFollowUpQuestionCommand(900L, 300L));
        var again = service.handle(new GenerateFollowUpQuestionCommand(900L, 300L));

        assertThat(again.statement()).isEqualTo("¿Qué resultado obtuviste?");
        verify(followUpQuestions, times(1)).generate(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void noFollowUpQuestionWhenTheQuestionDoesNotAllowIt() {
        sessionWithAnswer(false);

        assertThatThrownBy(() -> service.handle(new GenerateFollowUpQuestionCommand(900L, 300L)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("does not allow follow-up");
        verifyNoInteractions(followUpQuestions);
    }

    @Test
    void noFollowUpQuestionForAnUnknownAnswer() {
        sessionWithAnswer(true);

        assertThatThrownBy(() -> service.handle(new GenerateFollowUpQuestionCommand(900L, 999L)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("parent answer");
        verifyNoInteractions(followUpQuestions);
    }

    @Test
    void unknownSessionIsNotFound() {
        assertThatThrownBy(() -> service.handle(new GenerateFollowUpQuestionCommand(901L, 300L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
