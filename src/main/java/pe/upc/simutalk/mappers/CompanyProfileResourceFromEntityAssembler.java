package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.CompanyProfileResource;
import pe.upc.simutalk.entities.CompanyProfile;

public class CompanyProfileResourceFromEntityAssembler {

    public static CompanyProfileResource toResourceFromEntity(CompanyProfile entity) {
        return new CompanyProfileResource(entity.getId(), entity.getUserId(), entity.getLegalName(),
                entity.getTradeName(), entity.getIndustry(), entity.getRuc().value(), entity.getCompanySize(),
                entity.getDistrict(), entity.getEmail() == null ? null : entity.getEmail().value(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
