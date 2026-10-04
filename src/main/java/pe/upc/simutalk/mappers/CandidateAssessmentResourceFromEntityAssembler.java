package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.CandidateAssessmentResource;
import pe.upc.simutalk.dtos.CandidateCriterionScoreResource;
import pe.upc.simutalk.entities.Assessment;

/** Builds the candidate view: integrity flags, confidence, engine and ids of other records are left out on purpose. */
public class CandidateAssessmentResourceFromEntityAssembler {

    public static CandidateAssessmentResource toResourceFromEntity(Assessment entity) {
        var scores = entity.getCriterionScores().stream()
                .map(score -> new CandidateCriterionScoreResource(score.getCriterionName(), score.getCriterionKind(),
                        score.getWeightApplied(), score.getScore()))
                .toList();
        return new CandidateAssessmentResource(entity.getInterviewSessionId(), entity.getJobPostingId(),
                entity.getWeightedScore(), entity.getComputedAt(), scores, entity.getFeedbackSummary());
    }
}
