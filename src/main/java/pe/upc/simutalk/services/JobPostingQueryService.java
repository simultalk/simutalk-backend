package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.GetJobPostingByIdQuery;
import pe.upc.simutalk.dtos.GetJobPostingIdsByCompanyIdQuery;
import pe.upc.simutalk.dtos.GetPublishedJobPostingCountByCompanyIdQuery;
import pe.upc.simutalk.dtos.SearchJobPostingsQuery;
import pe.upc.simutalk.entities.JobPosting;

import java.util.List;
import java.util.Optional;

public interface JobPostingQueryService {

    Optional<JobPosting> handle(GetJobPostingByIdQuery query);

    List<JobPosting> handle(SearchJobPostingsQuery query);

    long handle(GetPublishedJobPostingCountByCompanyIdQuery query);

    List<Long> handle(GetJobPostingIdsByCompanyIdQuery query);
}
