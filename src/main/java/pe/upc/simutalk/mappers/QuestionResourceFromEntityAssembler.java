package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.QuestionResource;
import pe.upc.simutalk.entities.Question;

public class QuestionResourceFromEntityAssembler {

    public static QuestionResource toResourceFromEntity(Question entity) {
        return new QuestionResource(entity.getId(), entity.getJobPostingId(), entity.getCriterionId(),
                entity.getStatement(), entity.getMaxDurationSeconds(), entity.getPosition(), entity.getOrigin(),
                entity.isAllowsFollowUp(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
