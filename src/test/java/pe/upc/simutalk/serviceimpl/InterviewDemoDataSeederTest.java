package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CreateInterviewSessionCommand;
import pe.upc.simutalk.dtos.GetInterviewSessionByApplicationIdQuery;
import pe.upc.simutalk.dtos.GetQuestionsByJobPostingIdQuery;
import pe.upc.simutalk.dtos.RecordAnswerCommand;
import pe.upc.simutalk.dtos.StartInterviewSessionCommand;
import pe.upc.simutalk.entities.Answer;
import pe.upc.simutalk.entities.InterviewSession;
import pe.upc.simutalk.entities.Question;
import pe.upc.simutalk.services.IamContextFacade;
import pe.upc.simutalk.services.InterviewSessionCommandService;
import pe.upc.simutalk.services.InterviewSessionQueryService;
import pe.upc.simutalk.services.ProfilesContextFacade;
import pe.upc.simutalk.services.QuestionCommandService;
import pe.upc.simutalk.services.QuestionQueryService;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InterviewDemoDataSeederTest {

    private static final long JOB_POSTING_ID = 5L;
    private static final long ROSA_APPLICATION = 100L;
    private static final long CARMEN_APPLICATION = 101L;

    private final QuestionCommandService questionCommandService = mock(QuestionCommandService.class);
    private final QuestionQueryService questionQueryService = mock(QuestionQueryService.class);
    private final InterviewSessionCommandService sessionCommandService = mock(InterviewSessionCommandService.class);
    private final InterviewSessionQueryService sessionQueryService = mock(InterviewSessionQueryService.class);
    private final IamContextFacade iam = mock(IamContextFacade.class);
    private final ProfilesContextFacade profiles = mock(ProfilesContextFacade.class);
    private final RecruitmentContextFacade recruitment = mock(RecruitmentContextFacade.class);

    private final InterviewDemoDataSeeder seeder = new InterviewDemoDataSeeder(true, questionCommandService,
            questionQueryService, sessionCommandService, sessionQueryService, iam, profiles, recruitment,
            new TransactionTemplate(mock(PlatformTransactionManager.class)));

    @BeforeEach
    void demoJobPostingIsPublishedWithTwoApplications() {
        when(iam.fetchUserIdByUsername("consultora.andina")).thenReturn(1L);
        when(profiles.fetchCompanyIdByUserId(1L)).thenReturn(10L);
        when(recruitment.fetchJobPostingIdsByCompanyId(10L)).thenReturn(List.of(JOB_POSTING_ID));
        when(recruitment.isJobPostingPublished(JOB_POSTING_ID)).thenReturn(true);
        when(recruitment.fetchApplicationIds(JOB_POSTING_ID)).thenReturn(List.of(ROSA_APPLICATION, CARMEN_APPLICATION));
        when(sessionQueryService.handle(any(GetInterviewSessionByApplicationIdQuery.class))).thenReturn(Optional.empty());
        candidate("rosa.quispe", 2L, 20L, ROSA_APPLICATION);
        candidate("carmen.ramos", 3L, 30L, CARMEN_APPLICATION);
    }

    @Test
    void createsNoSessionForApplicationsThatAreNoLongerReceived() {
        when(recruitment.fetchApplicationStatus(ROSA_APPLICATION)).thenReturn("ASSESSED");
        when(recruitment.fetchApplicationStatus(CARMEN_APPLICATION)).thenReturn("INTERVIEWING");

        seeder.runInterviews(null);

        verify(sessionCommandService, never()).handle(any(CreateInterviewSessionCommand.class));
    }

    @Test
    void createsSessionsOnlyForReceivedApplications() {
        when(recruitment.fetchApplicationStatus(ROSA_APPLICATION)).thenReturn("ASSESSED");
        when(recruitment.fetchApplicationStatus(CARMEN_APPLICATION)).thenReturn("RECEIVED");
        var session = mock(InterviewSession.class);
        when(session.getId()).thenReturn(900L);
        when(sessionCommandService.handle(any(CreateInterviewSessionCommand.class))).thenReturn(session);
        when(sessionCommandService.handle(any(RecordAnswerCommand.class))).thenReturn(mock(Answer.class));
        var script = List.of(question(1L), question(2L), question(3L));
        when(questionQueryService.handle(any(GetQuestionsByJobPostingIdQuery.class))).thenReturn(script);

        seeder.runInterviews(null);

        verify(sessionCommandService).handle(argThat((CreateInterviewSessionCommand command) ->
                command.applicationId().equals(CARMEN_APPLICATION)));
        verify(sessionCommandService, times(1)).handle(any(CreateInterviewSessionCommand.class));
        verify(sessionCommandService).handle(new StartInterviewSessionCommand(900L));
    }

    @Test
    void skipsWhenTheDemoJobPostingAlreadyHasSessions() {
        when(sessionQueryService.handle(new GetInterviewSessionByApplicationIdQuery(ROSA_APPLICATION)))
                .thenReturn(Optional.of(mock(InterviewSession.class)));

        seeder.runInterviews(null);

        verify(recruitment, never()).fetchApplicationStatus(any());
        verify(sessionCommandService, never()).handle(any(CreateInterviewSessionCommand.class));
    }

    @Test
    void aFailureIsLoggedAndNeverPropagates() {
        when(recruitment.fetchApplicationStatus(any())).thenReturn("RECEIVED");
        when(sessionCommandService.handle(any(CreateInterviewSessionCommand.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatCode(() -> seeder.runInterviews(null)).doesNotThrowAnyException();
    }

    @Test
    void aFailureWritingTheScriptIsLoggedAndNeverPropagates() {
        when(recruitment.isJobPostingDraft(JOB_POSTING_ID)).thenThrow(new IllegalStateException("boom"));

        assertThatCode(() -> seeder.writeScript(null)).doesNotThrowAnyException();
    }

    @Test
    void doesNothingWhenDemoDataIsDisabled() {
        var disabled = new InterviewDemoDataSeeder(false, questionCommandService, questionQueryService,
                sessionCommandService, sessionQueryService, iam, profiles, recruitment,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));

        disabled.runInterviews(null);
        disabled.writeScript(null);

        verifyNoInteractions(recruitment, sessionCommandService, questionCommandService);
    }

    private void candidate(String username, Long userId, Long candidateId, Long applicationId) {
        when(iam.fetchUserIdByUsername(username)).thenReturn(userId);
        when(profiles.fetchCandidateIdByUserId(userId)).thenReturn(candidateId);
        when(recruitment.fetchCandidateIdByApplicationId(applicationId)).thenReturn(candidateId);
    }

    private static Question question(Long id) {
        var question = mock(Question.class);
        when(question.getId()).thenReturn(id);
        return question;
    }
}
