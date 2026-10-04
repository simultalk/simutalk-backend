package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CriterionAverageView;

import java.util.List;

/**
 * Contract other bounded contexts (analytics) use to read aggregated assessment figures.
 * Implemented by assessment with database aggregations.
 */
public interface AssessmentContextFacade {

    /** Average score per criterion over the assessed candidates of the job posting. */
    List<CriterionAverageView> fetchCriterionAverages(Long jobPostingId);

    long countAssessmentsByJobPostingIds(List<Long> jobPostingIds);

    /** Evidences anchored in the assessments of the given job postings. */
    long countEvidencesByJobPostingIds(List<Long> jobPostingIds);
}
