package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.ApplicationResource;
import pe.upc.simutalk.entities.Application;

public class ApplicationResourceFromEntityAssembler {

    public static ApplicationResource toResourceFromEntity(Application entity) {
        return new ApplicationResource(entity.getId(), entity.getJobPostingId(), entity.getCandidateId(),
                entity.getStatus(), entity.getAppliedAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
