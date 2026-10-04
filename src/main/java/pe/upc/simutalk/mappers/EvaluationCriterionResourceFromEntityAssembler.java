package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.EvaluationCriterionResource;
import pe.upc.simutalk.entities.EvaluationCriterion;

public class EvaluationCriterionResourceFromEntityAssembler {

    public static EvaluationCriterionResource toResourceFromEntity(EvaluationCriterion entity) {
        return new EvaluationCriterionResource(entity.getId(), entity.getJobPostingId(), entity.getName(),
                entity.getDescription(), entity.getWeight().value(), entity.getCriterionType(),
                entity.getCertificationName(), entity.isMandatory(), entity.getOrigin(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
