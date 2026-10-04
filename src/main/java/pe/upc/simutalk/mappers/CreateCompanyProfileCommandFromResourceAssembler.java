package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.CreateCompanyProfileCommand;
import pe.upc.simutalk.dtos.CreateCompanyProfileResource;

public class CreateCompanyProfileCommandFromResourceAssembler {

    public static CreateCompanyProfileCommand toCommandFromResource(CreateCompanyProfileResource resource) {
        return new CreateCompanyProfileCommand(resource.userId(), resource.legalName(), resource.tradeName(),
                resource.industry(), resource.ruc(), resource.companySize(), resource.district(), resource.email());
    }
}
