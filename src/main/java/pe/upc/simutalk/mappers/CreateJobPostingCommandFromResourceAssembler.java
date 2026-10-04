package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.CreateJobPostingCommand;
import pe.upc.simutalk.dtos.CreateJobPostingResource;

public class CreateJobPostingCommandFromResourceAssembler {

    /**
     * @param companyId company of the authenticated recruiter; never taken from the request body
     */
    public static CreateJobPostingCommand toCommandFromResource(CreateJobPostingResource resource, Long companyId) {
        return new CreateJobPostingCommand(resource.title(), resource.description(), companyId,
                resource.closingDate(), resource.anonymizedScreening());
    }
}
