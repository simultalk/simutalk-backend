package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CandidatePersonalData;
import pe.upc.simutalk.dtos.GetEvidencesByCriterionScoreQuery;
import pe.upc.simutalk.dtos.GetRankingByJobPostingIdQuery;
import pe.upc.simutalk.dtos.InterviewSessionSnapshot;
import pe.upc.simutalk.entities.Assessment;
import pe.upc.simutalk.entities.CriterionScore;
import pe.upc.simutalk.entities.Evidence;
import pe.upc.simutalk.enums.CriterionKind;
import pe.upc.simutalk.repositories.AssessmentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RankingAnonymizationTest {

    private final AssessmentRepository repository = mock(AssessmentRepository.class);
    private final AssessmentExternalContextsService contexts = mock(AssessmentExternalContextsService.class);
    private final AssessmentQueryServiceImpl service =
            new AssessmentQueryServiceImpl(repository, contexts, new TranscriptAnonymizer(), "test-secret");

    private Assessment rosa;
    private Assessment jorge;

    private static Assessment assessment(long id, long candidateId, String score, String excerpt) {
        var competency = new CriterionScore(1L, "Pensamiento analítico", CriterionKind.COMPETENCY, new BigDecimal(score),
                100, new BigDecimal("0.8"), List.of(new Evidence(300L + id, excerpt, 0, excerpt.length())));
        ReflectionTestUtils.setField(competency, "id", 10L + id);
        var assessment = Assessment.calculate(new InterviewSessionSnapshot(id, 50L + id, 10L, candidateId, "COMPLETED"),
                List.of(competency), List.of(), "mock-1", Instant.now());
        ReflectionTestUtils.setField(assessment, "id", id);
        return assessment;
    }

    @BeforeEach
    void setUp() {
        rosa = assessment(1, 7L, "7.0", "Como dije, soy Rosa y separaría la caída por región.");
        jorge = assessment(2, 8L, "9.0", "Compararía contra el mismo mes del año anterior.");
        when(contexts.existsJobPosting(10L)).thenReturn(true);
        when(contexts.fetchCriteria(10L)).thenReturn(List.of());
        when(repository.findAllWithScoresByJobPostingId(10L)).thenReturn(List.of(rosa, jorge));
        when(repository.findWithScoresById(1L)).thenReturn(Optional.of(rosa));
        when(contexts.fetchCandidatePersonalData(7L)).thenReturn(Optional.of(new CandidatePersonalData(7L, "Rosa",
                "Quispe Mamani", "45879632", "+51987654321", "Comas", LocalDate.of(1996, 3, 14))));
        when(contexts.fetchCandidatePersonalData(8L)).thenReturn(Optional.of(new CandidatePersonalData(8L, "Jorge",
                "Huamán Torres", "70214589", "+51912345678", "Comas", LocalDate.of(1993, 11, 2))));
    }

    @Test
    void rankingShowsIdentityWhenNotAnonymized() {
        var ranking = service.handle(new GetRankingByJobPostingIdQuery(10L, false));

        assertThat(ranking.anonymized()).isFalse();
        assertThat(ranking.entries()).extracting(entry -> entry.fullName()).containsExactly("Jorge Huamán Torres", "Rosa Quispe Mamani");
        assertThat(ranking.entries().get(0).rank()).isEqualTo(1);
        assertThat(ranking.entries().get(0).candidateCode()).matches("CANDIDATO-[A-Z]-\\d{4}");
    }

    @Test
    void anonymizedOnRequestHidesNameDocumentAndCandidateId() {
        var ranking = service.handle(new GetRankingByJobPostingIdQuery(10L, true));

        assertThat(ranking.anonymized()).isTrue();
        assertThat(ranking.anonymizationForced()).isFalse();
        ranking.entries().forEach(entry -> {
            assertThat(entry.fullName()).isNull();
            assertThat(entry.documentNumber()).isNull();
            assertThat(entry.candidateId()).isNull();
            assertThat(entry.candidateCode()).matches("CANDIDATO-[A-Z]-\\d{4}");
        });
    }

    @Test
    void anonymizedScreeningForcesAnonymizationEvenIfNotRequested() {
        when(contexts.isAnonymizedScreening(10L)).thenReturn(true);

        var ranking = service.handle(new GetRankingByJobPostingIdQuery(10L, false));

        assertThat(ranking.anonymized()).isTrue();
        assertThat(ranking.anonymizationForced()).isTrue();
        assertThat(ranking.entries()).allSatisfy(entry -> {
            assertThat(entry.fullName()).isNull();
            assertThat(entry.documentNumber()).isNull();
        });
    }

    @Test
    void candidateCodesAreStableAcrossCalls() {
        var first = service.handle(new GetRankingByJobPostingIdQuery(10L, true));
        var second = service.handle(new GetRankingByJobPostingIdQuery(10L, true));

        assertThat(first.entries()).extracting(entry -> entry.candidateCode())
                .containsExactlyElementsOf(second.entries().stream().map(entry -> entry.candidateCode()).toList());
    }

    @Test
    void evidencesAreRedactedWhenAnonymizationIsForced() {
        when(contexts.isAnonymizedScreening(10L)).thenReturn(true);

        var evidences = service.handle(new GetEvidencesByCriterionScoreQuery(1L, 11L));

        assertThat(evidences).singleElement().satisfies(evidence -> {
            assertThat(evidence.getExcerpt()).doesNotContain("Rosa").contains("[NOMBRE]");
            assertThat(evidence.getStartOffset()).isZero();
        });
    }
}
