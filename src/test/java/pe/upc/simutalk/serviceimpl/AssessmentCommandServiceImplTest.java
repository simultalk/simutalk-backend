package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.services.AnswerScoringService.CriterionFeedback;
import pe.upc.simutalk.services.AnswerScoringService.ScoringResult;

import pe.upc.simutalk.dtos.CandidatePersonalData;
import pe.upc.simutalk.dtos.ComputeAssessmentCommand;
import pe.upc.simutalk.dtos.CriterionView;
import pe.upc.simutalk.dtos.InterviewAnswerView;
import pe.upc.simutalk.dtos.InterviewSessionSnapshot;
import pe.upc.simutalk.enums.CriterionKind;
import pe.upc.simutalk.enums.IntegrityFlagType;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.repositories.AssessmentRepository;
import pe.upc.simutalk.services.AnswerScoringService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AssessmentCommandServiceImplTest {

    private final AssessmentRepository repository = mock(AssessmentRepository.class);
    private final AssessmentExternalContextsService contexts = mock(AssessmentExternalContextsService.class);
    private final AnswerScoringService scoring = mock(AnswerScoringService.class);
    private final AssessmentCommandServiceImpl service =
            new AssessmentCommandServiceImpl(repository, contexts, new TranscriptAnonymizer(), scoring);
    private final List<String> transcriptsSentToProvider = new ArrayList<>();

    private static final String ANSWER = "Soy Rosa Quispe. Separaría la caída en volumen y ticket promedio por región.";

    @BeforeEach
    void setUp() {
        when(contexts.fetchSession(900L)).thenReturn(new InterviewSessionSnapshot(900L, 50L, 10L, 7L, "COMPLETED"));
        when(contexts.fetchCriteria(10L)).thenReturn(List.of(
                new CriterionView(1L, "Pensamiento analítico", "Resuelve problemas con datos.", 70, "COMPETENCY", null, false),
                new CriterionView(2L, "Certificación", "Análisis de datos", 30, "CERTIFICATION", "Google Data Analytics", false)));
        when(contexts.fetchAnswers(900L)).thenReturn(List.of(new InterviewAnswerView(300L, 5L, 1L, ANSWER, false)));
        when(contexts.fetchCandidatePersonalData(7L)).thenReturn(Optional.of(new CandidatePersonalData(7L, "Rosa",
                "Quispe Mamani", "45879632", "+51987654321", "Comas", LocalDate.of(1996, 3, 14))));
        when(contexts.fetchVerifiedCertificationCount(7L)).thenReturn(1L);
        when(scoring.engineVersion()).thenReturn("mock-1");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(scoring.score(anyString(), anyString(), any())).thenAnswer(invocation -> {
            String anonymized = invocation.getArgument(0);
            transcriptsSentToProvider.add(anonymized);
            var excerpt = "Separaría la caída en volumen y ticket promedio por región.";
            var start = anonymized.indexOf(excerpt);
            return new ScoringResult(new BigDecimal("8.0"), new BigDecimal("0.80"), excerpt, start, start + excerpt.length(), false);
        });
    }

    @Test
    void computesWeightedScoreWithEvidenceAnchoredInTheOriginalTranscript() {
        var assessment = service.handle(new ComputeAssessmentCommand(900L));

        // (8.0 * 70 + 7.0 * 30) / 100 = 7.7 ; one verified certification scores 7.0
        assertThat(assessment.getWeightedScore()).isEqualByComparingTo("7.7");
        var competency = assessment.getCriterionScores().get(0);
        assertThat(competency.getCriterionKind()).isEqualTo(CriterionKind.COMPETENCY);
        var evidence = competency.getEvidences().get(0);
        assertThat(ANSWER.substring(evidence.getStartOffset(), evidence.getEndOffset())).isEqualTo(evidence.getExcerpt());
        assertThat(assessment.getCriterionScores().get(1).getEvidences()).isEmpty();
        assertThat(transcriptsSentToProvider).singleElement().asString().doesNotContain("Rosa", "Quispe").contains("[NOMBRE]");
    }

    @Test
    void failsWithoutEvidenceWhenTheProviderIsUnavailable() {
        when(scoring.score(anyString(), anyString(), any())).thenReturn(ScoringResult.unavailable());

        assertThatThrownBy(() -> service.handle(new ComputeAssessmentCommand(900L)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("No evidence");
        verify(repository, never()).save(any());
    }

    @Test
    void refusesSessionsThatAreNotCompleted() {
        when(contexts.fetchSession(900L)).thenReturn(new InterviewSessionSnapshot(900L, 50L, 10L, 7L, "IN_PROGRESS"));

        assertThatThrownBy(() -> service.handle(new ComputeAssessmentCommand(900L)))
                .isInstanceOf(BusinessRuleViolationException.class);
        verifyNoInteractions(scoring);
    }

    @Test
    void refusesToAssessTwice() {
        when(repository.existsByInterviewSessionId(900L)).thenReturn(true);

        assertThatThrownBy(() -> service.handle(new ComputeAssessmentCommand(900L)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already");
    }

    @Test
    void raisesIntegrityFlags() {
        when(contexts.fetchRejectedCertificationCount(7L)).thenReturn(1L);
        when(scoring.score(anyString(), anyString(), any())).thenAnswer(invocation -> {
            String anonymized = invocation.getArgument(0);
            var excerpt = "Separaría la caída";
            var start = anonymized.indexOf(excerpt);
            return new ScoringResult(new BigDecimal("9.0"), new BigDecimal("0.9"), excerpt, start, start + excerpt.length(), true);
        });

        var assessment = service.handle(new ComputeAssessmentCommand(900L));

        assertThat(assessment.getIntegrityFlags()).extracting(flag -> flag.getFlagType())
                .containsExactly(IntegrityFlagType.AI_GENERATED_CONTENT, IntegrityFlagType.CV_INCONSISTENCY);
    }

    @Test
    void recordsFeedbackBuiltOnlyFromScoresAndAnonymizedExcerpts() {
        when(scoring.summarizeFeedback(any(), anyList())).thenReturn("Tu puntaje ponderado fue 7,7 de 10.");

        var assessment = service.handle(new ComputeAssessmentCommand(900L));

        assertThat(assessment.getFeedbackSummary()).isEqualTo("Tu puntaje ponderado fue 7,7 de 10.");
        var inputs = ArgumentCaptor.forClass(List.class);
        verify(scoring).summarizeFeedback(eq(new BigDecimal("7.7")), inputs.capture());
        @SuppressWarnings("unchecked")
        List<CriterionFeedback> criteria = inputs.getValue();
        assertThat(criteria).extracting(CriterionFeedback::criterionName)
                .containsExactly("Pensamiento analítico", "Certificación");
        assertThat(criteria.get(0).anonymizedExcerpt()).isEqualTo("Separaría la caída en volumen y ticket promedio por región.");
        assertThat(criteria.get(1).anonymizedExcerpt()).isNull();
        assertThat(criteria.toString()).doesNotContain("Rosa", "Quispe", "45879632", "Comas");
    }
}
