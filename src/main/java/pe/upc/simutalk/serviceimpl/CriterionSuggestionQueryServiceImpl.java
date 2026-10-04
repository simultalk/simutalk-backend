package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CriterionSuggestion;
import pe.upc.simutalk.dtos.GetCriterionSuggestionsQuery;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.repositories.JobPostingRepository;
import pe.upc.simutalk.services.CriterionSuggestionQueryService;
import pe.upc.simutalk.services.CriterionSuggestionService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads the job posting, lets the aggregate check that criteria can still be suggested (DRAFT) and asks
 * the suggestion port. Not transactional on purpose: no database transaction is kept open while the AI
 * provider answers. Nothing is persisted: the recruiter accepts suggestions through the criteria endpoint.
 */
@Service
@RequiredArgsConstructor
public class CriterionSuggestionQueryServiceImpl implements CriterionSuggestionQueryService {

    private final JobPostingRepository jobPostingRepository;
    private final CriterionSuggestionService criterionSuggestionService;

    @Override
    public List<CriterionSuggestion> handle(GetCriterionSuggestionsQuery query) {
        var jobPosting = jobPostingRepository.findWithCriteriaById(query.jobPostingId())
                .orElseThrow(() -> new ResourceNotFoundException("Job posting", query.jobPostingId()));
        jobPosting.ensureCriteriaCanBeSuggested();
        return criterionSuggestionService.suggest(jobPosting.getTitle(), jobPosting.getDescription(),
                jobPosting.getCriterionNames());
    }
}
