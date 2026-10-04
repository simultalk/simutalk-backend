package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.JobPostingResource;
import pe.upc.simutalk.entities.JobPosting;

public class JobPostingResourceFromEntityAssembler {

    public static JobPostingResource toResourceFromEntity(JobPosting entity) {
        var criteria = entity.getCriteria().stream()
                .map(EvaluationCriterionResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return new JobPostingResource(entity.getId(), entity.getTitle(), entity.getDescription(),
                entity.getCompanyId().value(), entity.getStatus(), entity.getClosingDate(),
                entity.isAnonymizedScreening(), entity.getTotalWeight(), criteria,
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
