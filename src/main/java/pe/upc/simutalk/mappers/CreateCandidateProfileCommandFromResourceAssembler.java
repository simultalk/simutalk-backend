package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.CreateCandidateProfileCommand;
import pe.upc.simutalk.dtos.CreateCandidateProfileResource;

public class CreateCandidateProfileCommandFromResourceAssembler {

    public static CreateCandidateProfileCommand toCommandFromResource(CreateCandidateProfileResource resource) {
        return new CreateCandidateProfileCommand(resource.userId(), resource.firstName(), resource.lastName(),
                resource.documentNumber(), resource.birthDate(), resource.phone(), resource.district(),
                resource.yearsOfExperience(), resource.email());
    }
}
