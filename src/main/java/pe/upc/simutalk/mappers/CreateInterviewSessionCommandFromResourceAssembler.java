package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.CreateInterviewSessionCommand;
import pe.upc.simutalk.dtos.CreateInterviewSessionResource;

public class CreateInterviewSessionCommandFromResourceAssembler {

    public static CreateInterviewSessionCommand toCommandFromResource(Long applicationId,
                                                                      CreateInterviewSessionResource resource) {
        return new CreateInterviewSessionCommand(applicationId, resource.expiresAt());
    }
}
