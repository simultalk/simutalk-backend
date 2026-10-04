package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.GetJobPostingByIdQuery;
import pe.upc.simutalk.dtos.GetJobPostingIdsByCompanyIdQuery;
import pe.upc.simutalk.dtos.GetPublishedJobPostingCountByCompanyIdQuery;
import pe.upc.simutalk.dtos.SearchJobPostingsQuery;
import pe.upc.simutalk.entities.CompanyId;
import pe.upc.simutalk.entities.JobPosting;
import pe.upc.simutalk.enums.JobPostingStatus;
import pe.upc.simutalk.repositories.JobPostingRepository;
import pe.upc.simutalk.services.JobPostingQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class JobPostingQueryServiceImpl implements JobPostingQueryService {

    private final JobPostingRepository jobPostingRepository;

    @Override
    public Optional<JobPosting> handle(GetJobPostingByIdQuery query) {
        return jobPostingRepository.findWithCriteriaById(query.jobPostingId())
                .filter(jobPosting -> jobPosting.isVisibleTo(query.viewer()));
    }

    @Override
    public List<JobPosting> handle(SearchJobPostingsQuery query) {
        return findByFilters(query).stream()
                .filter(jobPosting -> jobPosting.isListedFor(query.viewer()))
                .toList();
    }

    @Override
    public long handle(GetPublishedJobPostingCountByCompanyIdQuery query) {
        return jobPostingRepository.countByCompanyIdAndStatus(new CompanyId(query.companyId()), JobPostingStatus.PUBLISHED);
    }

    @Override
    public List<Long> handle(GetJobPostingIdsByCompanyIdQuery query) {
        return jobPostingRepository.findIdsByCompanyId(new CompanyId(query.companyId()));
    }

    private List<JobPosting> findByFilters(SearchJobPostingsQuery query) {
        var status = query.status();
        if (query.companyId() == null) {
            return status == null
                    ? jobPostingRepository.findAllByOrderByIdAsc()
                    : jobPostingRepository.findAllByStatusOrderByIdAsc(status);
        }
        var companyId = new CompanyId(query.companyId());
        return status == null
                ? jobPostingRepository.findAllByCompanyIdOrderByIdAsc(companyId)
                : jobPostingRepository.findAllByCompanyIdAndStatusOrderByIdAsc(companyId, status);
    }
}
