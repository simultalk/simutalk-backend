package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.UpdateCandidateProfileCommand;
import pe.upc.simutalk.dtos.UpdateCandidateProfileResource;

public class UpdateCandidateProfileCommandFromResourceAssembler {

    public static UpdateCandidateProfileCommand toCommandFromResource(Long candidateId,
                                                                      UpdateCandidateProfileResource resource) {
        return new UpdateCandidateProfileCommand(candidateId, resource.firstName(), resource.lastName(),
                resource.birthDate(), resource.phone(), resource.district(), resource.yearsOfExperience(), resource.email());
    }
}
