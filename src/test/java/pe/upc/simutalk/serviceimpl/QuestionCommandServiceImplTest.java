package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CreateQuestionCommand;
import pe.upc.simutalk.dtos.DeleteQuestionCommand;
import pe.upc.simutalk.dtos.ReorderQuestionsCommand;
import pe.upc.simutalk.dtos.UpdateQuestionCommand;
import pe.upc.simutalk.entities.Question;
import pe.upc.simutalk.enums.QuestionOrigin;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.repositories.QuestionRepository;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuestionCommandServiceImplTest {

    private static final long DRAFT_POSTING = 1L;
    private static final long PUBLISHED_POSTING = 2L;
    private static final long COMPETENCY = 11L;
    private static final long CERTIFICATION = 12L;

    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final RecruitmentContextFacade recruitment = mock(RecruitmentContextFacade.class);
    private final QuestionCommandServiceImpl service =
            new QuestionCommandServiceImpl(questions, new ExternalRecruitmentService(recruitment));
    private final List<Question> script = new ArrayList<>();

    private Question stored(long id, int position) {
        var question = new Question(DRAFT_POSTING, COMPETENCY, "Pregunta " + id, 120, position, QuestionOrigin.MANUAL, false);
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    @BeforeEach
    void setUp() {
        when(recruitment.existsJobPostingById(anyLong())).thenReturn(false);
        when(recruitment.existsJobPostingById(DRAFT_POSTING)).thenReturn(true);
        when(recruitment.existsJobPostingById(PUBLISHED_POSTING)).thenReturn(true);
        when(recruitment.isJobPostingDraft(DRAFT_POSTING)).thenReturn(true);
        when(recruitment.fetchCompetencyCriterionIds(DRAFT_POSTING)).thenReturn(List.of(COMPETENCY));
        when(recruitment.existsCriterionInJobPosting(DRAFT_POSTING, COMPETENCY)).thenReturn(true);
        when(recruitment.existsCriterionInJobPosting(DRAFT_POSTING, CERTIFICATION)).thenReturn(true);
        when(questions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        script.addAll(List.of(stored(1, 1), stored(2, 2), stored(3, 3)));
        when(questions.findAllByJobPostingIdOrderByPositionAscIdAsc(DRAFT_POSTING)).thenReturn(script);
        when(questions.countByJobPostingId(DRAFT_POSTING)).thenReturn(3L);
        script.forEach(question -> when(questions.findById(question.getId())).thenReturn(Optional.of(question)));
    }

    private static CreateQuestionCommand create(long jobPostingId, long criterionId) {
        return new CreateQuestionCommand(jobPostingId, criterionId, "¿Cómo validarías un reporte de ventas?", 120,
                QuestionOrigin.MANUAL, false);
    }

    @Test
    void appendsNewQuestionAtTheEndOfTheScript() {
        var question = service.handle(create(DRAFT_POSTING, COMPETENCY));

        assertThat(question.getPosition()).isEqualTo(4);
    }

    @Test
    void certificationCriterionIsRejectedWithExplicitMessage() {
        assertThatThrownBy(() -> service.handle(create(DRAFT_POSTING, CERTIFICATION)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("CERTIFICATION")
                .hasMessageContaining("not evaluated by interview");
        verify(questions, never()).save(any());
    }

    @Test
    void criterionOfAnotherJobPostingIsRejected() {
        assertThatThrownBy(() -> service.handle(create(DRAFT_POSTING, 99L)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void scriptIsFrozenOncePublished() {
        assertThatThrownBy(() -> service.handle(create(PUBLISHED_POSTING, COMPETENCY)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void unknownJobPostingIsNotFound() {
        assertThatThrownBy(() -> service.handle(create(404L, COMPETENCY))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateOfAQuestionFromAnotherJobPostingIsNotFound() {
        assertThatThrownBy(() -> service.handle(new UpdateQuestionCommand(PUBLISHED_POSTING, 1L, COMPETENCY, "x", 120,
                QuestionOrigin.MANUAL, false))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRenumbersTheRemainingQuestions() {
        service.handle(new DeleteQuestionCommand(DRAFT_POSTING, 1L));

        verify(questions).delete(script.get(0));
        assertThat(script.get(1).getPosition()).isEqualTo(1);
        assertThat(script.get(2).getPosition()).isEqualTo(2);
    }

    @Test
    void reorderAssignsConsecutivePositions() {
        var ordered = service.handle(new ReorderQuestionsCommand(DRAFT_POSTING, List.of(3L, 1L, 2L)));

        assertThat(ordered).extracting(Question::getId).containsExactly(3L, 1L, 2L);
        assertThat(ordered).extracting(Question::getPosition).containsExactly(1, 2, 3);
    }

    @Test
    void reorderMustListEveryQuestionExactlyOnce() {
        assertThatThrownBy(() -> service.handle(new ReorderQuestionsCommand(DRAFT_POSTING, List.of(3L, 1L))))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> service.handle(new ReorderQuestionsCommand(DRAFT_POSTING, List.of(3L, 1L, 1L))))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> service.handle(new ReorderQuestionsCommand(DRAFT_POSTING, List.of(3L, 1L, 9L))))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
