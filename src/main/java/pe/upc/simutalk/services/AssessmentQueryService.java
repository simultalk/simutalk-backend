package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CountAssessmentsByJobPostingIdsQuery;
import pe.upc.simutalk.dtos.CountEvidencesByJobPostingIdsQuery;
import pe.upc.simutalk.dtos.CriterionAverage;
import pe.upc.simutalk.dtos.GetAssessmentByIdQuery;
import pe.upc.simutalk.dtos.GetAssessmentByInterviewSessionIdQuery;
import pe.upc.simutalk.dtos.GetCriterionAveragesQuery;
import pe.upc.simutalk.dtos.GetEvidencesByCriterionScoreQuery;
import pe.upc.simutalk.dtos.GetRankingByJobPostingIdQuery;
import pe.upc.simutalk.dtos.Ranking;
import pe.upc.simutalk.entities.Assessment;
import pe.upc.simutalk.entities.Evidence;

import java.util.List;
import java.util.Optional;

public interface AssessmentQueryService {

    Optional<Assessment> handle(GetAssessmentByIdQuery query);

    Optional<Assessment> handle(GetAssessmentByInterviewSessionIdQuery query);

    /**
     * Evidences of a criterion score. When the job posting uses anonymized screening, the
     * candidate's personal data inside the excerpts is redacted.
     */
    List<Evidence> handle(GetEvidencesByCriterionScoreQuery query);

    Ranking handle(GetRankingByJobPostingIdQuery query);

    /** Average score per criterion, aggregated in the database; one decimal. */
    List<CriterionAverage> handle(GetCriterionAveragesQuery query);

    long handle(CountAssessmentsByJobPostingIdsQuery query);

    long handle(CountEvidencesByJobPostingIdsQuery query);
}
