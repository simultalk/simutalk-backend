package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.ChangeJobPostingStatusCommand;
import pe.upc.simutalk.dtos.UpdateJobPostingStatusResource;

public class ChangeJobPostingStatusCommandFromResourceAssembler {

    public static ChangeJobPostingStatusCommand toCommandFromResource(Long jobPostingId,
                                                                      UpdateJobPostingStatusResource resource) {
        return new ChangeJobPostingStatusCommand(jobPostingId, resource.status());
    }
}
