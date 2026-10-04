package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.InterviewSessionSnapshot;
import pe.upc.simutalk.entities.Assessment;
import pe.upc.simutalk.entities.CriterionScore;
import pe.upc.simutalk.entities.Evidence;
import pe.upc.simutalk.entities.IntegrityFlag;
import pe.upc.simutalk.enums.CriterionKind;
import pe.upc.simutalk.enums.FlagSeverity;
import pe.upc.simutalk.enums.IntegrityFlagType;
import pe.upc.simutalk.enums.RankingBadge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RankingPolicyTest {

    @ParameterizedTest
    @CsvSource({
            // requested, forced -> anonymized
            "false, false, false",
            "true,  false, true",
            "false, true,  true",
            "true,  true,  true"
    })
    void anonymizationIsForcedByTheJobPosting(boolean requested, boolean forced, boolean expected) {
        assertThat(RankingPolicy.isAnonymized(requested, forced)).isEqualTo(expected);
    }

    private static Assessment assessment(String competencyScore, String confidence, String certificationScore,
                                         List<IntegrityFlag> flags, Instant computedAt) {
        var competency = new CriterionScore(1L, "Pensamiento analítico", CriterionKind.COMPETENCY,
                new BigDecimal(competencyScore), 70, new BigDecimal(confidence),
                List.of(new Evidence(1L, "Separaría volumen de ticket.", 0, 28)));
        var certification = new CriterionScore(2L, "Certificación", CriterionKind.CERTIFICATION,
                new BigDecimal(certificationScore), 30, BigDecimal.ONE, List.of());
        return Assessment.calculate(new InterviewSessionSnapshot(1L, 1L, 10L, 7L, "COMPLETED"),
                List.of(competency, certification), flags, "mock-1", computedAt);
    }

    @Test
    void ordersByWeightedScoreDescendingThenByAssessmentTime() {
        var now = Instant.now();
        var low = assessment("5.0", "0.8", "7.0", List.of(), now);
        var highLate = assessment("9.0", "0.8", "7.0", List.of(), now.plusSeconds(60));
        var highEarly = assessment("9.0", "0.8", "7.0", List.of(), now);
        var list = new ArrayList<>(List.of(low, highLate, highEarly));

        list.sort(RankingPolicy.ORDER);

        assertThat(list).containsExactly(highEarly, highLate, low);
    }

    @Test
    void awardsExplainableBadges() {
        var flag = new IntegrityFlag(IntegrityFlagType.AI_GENERATED_CONTENT, FlagSeverity.MEDIUM, "Posible texto generado",
                Instant.now());
        var assessment = assessment("9.0", "0.80", "0.0", List.of(flag), Instant.now());

        var badges = RankingPolicy.badgesFor(assessment, 1, 0, Set.of(2L));

        assertThat(badges).containsExactly(RankingBadge.TOP_RANKED, RankingBadge.STRONG_EVIDENCE,
                RankingBadge.MISSING_MANDATORY_CERTIFICATION, RankingBadge.INTEGRITY_ALERT);
    }

    @Test
    void noBadgesForAnAverageCandidate() {
        var assessment = assessment("6.0", "0.60", "7.0", List.of(), Instant.now());

        assertThat(RankingPolicy.badgesFor(assessment, 3, 0, Set.of())).isEmpty();
        assertThat(RankingPolicy.badgesFor(assessment, 3, 2, Set.of(2L))).containsExactly(RankingBadge.VERIFIED_CERTIFICATIONS);
    }

    @ParameterizedTest
    @CsvSource({"0, 0.0", "1, 7.0", "2, 8.5", "3, 10.0", "9, 10.0"})
    void certificationScoreFromVerifiedCount(long count, String expected) {
        assertThat(CertificationScoringPolicy.scoreFor(count)).isEqualByComparingTo(expected);
    }
}
