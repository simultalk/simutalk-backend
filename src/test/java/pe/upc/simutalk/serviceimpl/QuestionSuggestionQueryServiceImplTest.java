package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CriterionView;
import pe.upc.simutalk.dtos.GetQuestionSuggestionsQuery;
import pe.upc.simutalk.dtos.QuestionSuggestion;
import pe.upc.simutalk.entities.Question;
import pe.upc.simutalk.enums.QuestionOrigin;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.repositories.QuestionRepository;
import pe.upc.simutalk.services.QuestionSuggestionService;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class QuestionSuggestionQueryServiceImplTest {

    private static final long DRAFT_POSTING = 1L;
    private static final long PUBLISHED_POSTING = 2L;
    private static final long COMPETENCY = 11L;
    private static final long CERTIFICATION = 12L;

    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final RecruitmentContextFacade recruitment = mock(RecruitmentContextFacade.class);
    private final QuestionSuggestionService suggestionPort = mock(QuestionSuggestionService.class);
    private final QuestionSuggestionQueryServiceImpl service = new QuestionSuggestionQueryServiceImpl(questions,
            new ExternalRecruitmentService(recruitment), suggestionPort);

    @BeforeEach
    void setUp() {
        var criteria = List.of(
                new CriterionView(COMPETENCY, "Pensamiento analítico", "Resuelve con datos", 70, "COMPETENCY", null, false),
                new CriterionView(CERTIFICATION, "Certificación", "Google Data Analytics", 30, "CERTIFICATION", "GDA", true));
        when(recruitment.existsJobPostingById(anyLong())).thenReturn(false);
        when(recruitment.existsJobPostingById(DRAFT_POSTING)).thenReturn(true);
        when(recruitment.existsJobPostingById(PUBLISHED_POSTING)).thenReturn(true);
        when(recruitment.isJobPostingDraft(DRAFT_POSTING)).thenReturn(true);
        when(recruitment.fetchCompetencyCriterionIds(DRAFT_POSTING)).thenReturn(List.of(COMPETENCY));
        when(recruitment.existsCriterionInJobPosting(DRAFT_POSTING, CERTIFICATION)).thenReturn(true);
        when(recruitment.fetchCriteria(DRAFT_POSTING)).thenReturn(criteria);
        when(recruitment.fetchJobPostingTitle(DRAFT_POSTING)).thenReturn("Analista de Datos");
        when(recruitment.fetchJobPostingDescription(DRAFT_POSTING)).thenReturn("SQL y Excel");
        when(questions.findAllByJobPostingIdOrderByPositionAscIdAsc(DRAFT_POSTING)).thenReturn(List.of(
                new Question(DRAFT_POSTING, COMPETENCY, "¿Qué es un LEFT JOIN?", 120, 1, QuestionOrigin.MANUAL, false)));
    }

    @Test
    void asksThePortWithThePostingTheCriterionAndTheExistingQuestionsAndPersistsNothing() {
        var proposed = List.of(new QuestionSuggestion("¿Cómo validas un reporte?", "Rigor"));
        when(suggestionPort.suggest("Analista de Datos", "SQL y Excel", "Pensamiento analítico", "Resuelve con datos",
                List.of("¿Qué es un LEFT JOIN?"))).thenReturn(proposed);

        var suggestions = service.handle(new GetQuestionSuggestionsQuery(DRAFT_POSTING, COMPETENCY));

        assertThat(suggestions).isEqualTo(proposed);
        verify(questions, never()).save(any());
    }

    @Test
    void questionsAreOnlySuggestedWhileThePostingIsDraft() {
        assertThatThrownBy(() -> service.handle(new GetQuestionSuggestionsQuery(PUBLISHED_POSTING, COMPETENCY)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DRAFT");
        verify(suggestionPort, never()).suggest(any(), any(), any(), any(), any());
    }

    @Test
    void unknownJobPostingIsNotFound() {
        assertThatThrownBy(() -> service.handle(new GetQuestionSuggestionsQuery(404L, COMPETENCY)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void certificationCriterionIsRejectedWithExplicitMessage() {
        assertThatThrownBy(() -> service.handle(new GetQuestionSuggestionsQuery(DRAFT_POSTING, CERTIFICATION)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("CERTIFICATION");
        verify(suggestionPort, never()).suggest(any(), any(), any(), any(), any());
    }

    @Test
    void criterionOfAnotherJobPostingIsRejected() {
        assertThatThrownBy(() -> service.handle(new GetQuestionSuggestionsQuery(DRAFT_POSTING, 99L)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("does not belong");
    }
}
