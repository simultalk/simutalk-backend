package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.GetAllCompanyProfilesQuery;
import pe.upc.simutalk.dtos.GetCompanyProfileByIdQuery;
import pe.upc.simutalk.dtos.GetCompanyProfileByUserIdQuery;
import pe.upc.simutalk.entities.CompanyProfile;
import pe.upc.simutalk.repositories.CompanyProfileRepository;
import pe.upc.simutalk.services.CompanyProfileQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CompanyProfileQueryServiceImpl implements CompanyProfileQueryService {

    private final CompanyProfileRepository companyProfileRepository;

    @Override
    public Optional<CompanyProfile> handle(GetCompanyProfileByIdQuery query) {
        return companyProfileRepository.findById(query.companyProfileId());
    }

    @Override
    public Page<CompanyProfile> handle(GetAllCompanyProfilesQuery query) {
        return companyProfileRepository.findAll(PageRequest.of(query.page(), query.size(), Sort.by("id")));
    }

    @Override
    public Optional<CompanyProfile> handle(GetCompanyProfileByUserIdQuery query) {
        return companyProfileRepository.findByUserId(query.userId());
    }
}
