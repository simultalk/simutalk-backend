package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.AddCertificationCommand;
import pe.upc.simutalk.dtos.CreateCertificationResource;

public class AddCertificationCommandFromResourceAssembler {

    public static AddCertificationCommand toCommandFromResource(Long candidateId, CreateCertificationResource resource) {
        return new AddCertificationCommand(candidateId, resource.title(), resource.issuer(),
                resource.credentialCode(), resource.issuedAt(), resource.expiresAt());
    }
}
