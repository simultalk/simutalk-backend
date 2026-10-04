package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.GetAllCompanyProfilesQuery;
import pe.upc.simutalk.dtos.GetCompanyProfileByIdQuery;
import pe.upc.simutalk.dtos.GetCompanyProfileByUserIdQuery;
import pe.upc.simutalk.entities.CompanyProfile;

import org.springframework.data.domain.Page;

import java.util.Optional;

public interface CompanyProfileQueryService {

    Optional<CompanyProfile> handle(GetCompanyProfileByIdQuery query);

    /** Page of companies ordered by id. */
    Page<CompanyProfile> handle(GetAllCompanyProfilesQuery query);

    Optional<CompanyProfile> handle(GetCompanyProfileByUserIdQuery query);
}
