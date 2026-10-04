package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CreateCompanyProfileCommand;
import pe.upc.simutalk.dtos.UpdateCompanyProfileCommand;
import pe.upc.simutalk.entities.CompanyProfile;

public interface CompanyProfileCommandService {

    CompanyProfile handle(CreateCompanyProfileCommand command);

    CompanyProfile handle(UpdateCompanyProfileCommand command);
}
