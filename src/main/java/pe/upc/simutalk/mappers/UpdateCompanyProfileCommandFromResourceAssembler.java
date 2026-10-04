package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.UpdateCompanyProfileCommand;
import pe.upc.simutalk.dtos.UpdateCompanyProfileResource;

public class UpdateCompanyProfileCommandFromResourceAssembler {

    public static UpdateCompanyProfileCommand toCommandFromResource(Long companyProfileId,
                                                                    UpdateCompanyProfileResource resource) {
        return new UpdateCompanyProfileCommand(companyProfileId, resource.legalName(), resource.tradeName(),
                resource.industry(), resource.companySize(), resource.district(), resource.email());
    }
}
