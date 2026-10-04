package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.AssessmentResource;
import pe.upc.simutalk.dtos.CriterionScoreResource;
import pe.upc.simutalk.dtos.IntegrityFlagResource;
import pe.upc.simutalk.entities.Assessment;

public class AssessmentResourceFromEntityAssembler {

    public static AssessmentResource toResourceFromEntity(Assessment entity) {
        var scores = entity.getCriterionScores().stream()
                .map(score -> new CriterionScoreResource(score.getId(), score.getCriterionId(), score.getCriterionName(),
                        score.getCriterionKind(), score.getScore(), score.getWeightApplied(), score.getConfidence(),
                        score.getEvidences().size()))
                .toList();
        var flags = entity.getIntegrityFlags().stream()
                .map(flag -> new IntegrityFlagResource(flag.getId(), flag.getFlagType(), flag.getSeverity(),
                        flag.getDetail(), flag.getRaisedAt()))
                .toList();
        return new AssessmentResource(entity.getId(), entity.getInterviewSessionId(), entity.getApplicationId(),
                entity.getJobPostingId(), entity.getWeightedScore(), entity.getEngineVersion(), entity.getComputedAt(),
                scores, flags, entity.getFeedbackSummary());
    }
}
