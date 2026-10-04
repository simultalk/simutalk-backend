package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.ChangeApplicationStatusCommand;
import pe.upc.simutalk.dtos.CriterionView;
import pe.upc.simutalk.dtos.GetApplicationByIdQuery;
import pe.upc.simutalk.dtos.GetApplicationCountByStatusQuery;
import pe.upc.simutalk.dtos.GetApplicationsByJobPostingIdQuery;
import pe.upc.simutalk.dtos.GetAverageTimeToShortlistQuery;
import pe.upc.simutalk.dtos.GetJobPostingByIdQuery;
import pe.upc.simutalk.dtos.GetJobPostingIdsByCompanyIdQuery;
import pe.upc.simutalk.dtos.GetPublishedJobPostingCountByCompanyIdQuery;
import pe.upc.simutalk.dtos.JobPostingViewer;
import pe.upc.simutalk.entities.Application;
import pe.upc.simutalk.entities.EvaluationCriterion;
import pe.upc.simutalk.entities.JobPosting;
import pe.upc.simutalk.enums.ApplicationStatus;
import pe.upc.simutalk.enums.CriterionType;
import pe.upc.simutalk.services.ApplicationCommandService;
import pe.upc.simutalk.services.ApplicationQueryService;
import pe.upc.simutalk.services.JobPostingQueryService;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * recruitment's implementation of the {@link RecruitmentContextFacade} contract published in
 * shared. Internal lookups are unrestricted: authorization is the caller's responsibility.
 */
@Service
@RequiredArgsConstructor
public class RecruitmentContextFacadeImpl implements RecruitmentContextFacade {

    private final JobPostingQueryService jobPostingQueryService;
    private final ApplicationQueryService applicationQueryService;
    private final ApplicationCommandService applicationCommandService;

    @Override
    public boolean existsJobPostingById(Long jobPostingId) {
        return findJobPosting(jobPostingId).isPresent();
    }

    @Override
    public boolean isJobPostingPublished(Long jobPostingId) {
        return findJobPosting(jobPostingId).map(JobPosting::isPublished).orElse(false);
    }

    @Override
    public boolean isJobPostingDraft(Long jobPostingId) {
        return findJobPosting(jobPostingId).map(JobPosting::isDraft).orElse(false);
    }

    @Override
    public Long fetchCompanyIdByJobPostingId(Long jobPostingId) {
        return findJobPosting(jobPostingId).map(jobPosting -> jobPosting.getCompanyId().value()).orElse(0L);
    }

    @Override
    public String fetchJobPostingTitle(Long jobPostingId) {
        return findJobPosting(jobPostingId).map(JobPosting::getTitle).orElse("");
    }

    @Override
    public String fetchJobPostingDescription(Long jobPostingId) {
        return findJobPosting(jobPostingId).map(JobPosting::getDescription).orElse("");
    }

    @Override
    public boolean existsCriterionInJobPosting(Long jobPostingId, Long criterionId) {
        return criterionId != null && findJobPosting(jobPostingId)
                .map(jobPosting -> jobPosting.getCriteria().stream()
                        .anyMatch(criterion -> criterionId.equals(criterion.getId())))
                .orElse(false);
    }

    @Override
    public List<CriterionView> fetchCriteria(Long jobPostingId) {
        return findJobPosting(jobPostingId)
                .map(jobPosting -> jobPosting.getCriteria().stream()
                        .map(criterion -> new CriterionView(criterion.getId(), criterion.getName(),
                                criterion.getDescription(), criterion.getWeight().value(),
                                criterion.getCriterionType().name(), criterion.getCertificationName(),
                                criterion.isMandatory()))
                        .toList())
                .orElse(List.of());
    }

    @Override
    public boolean isAnonymizedScreening(Long jobPostingId) {
        return findJobPosting(jobPostingId).map(JobPosting::isAnonymizedScreening).orElse(false);
    }

    @Override
    public List<Long> fetchCompetencyCriterionIds(Long jobPostingId) {
        return findJobPosting(jobPostingId)
                .map(jobPosting -> jobPosting.getCriteria().stream()
                        .filter(criterion -> criterion.getCriterionType() == CriterionType.COMPETENCY)
                        .map(EvaluationCriterion::getId)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public Long fetchJobPostingIdByApplicationId(Long applicationId) {
        return findApplication(applicationId).map(Application::getJobPostingId).orElse(0L);
    }

    @Override
    public List<Long> fetchApplicationIds(Long jobPostingId) {
        if (jobPostingId == null) {
            return List.of();
        }
        return applicationQueryService.handle(new GetApplicationsByJobPostingIdQuery(jobPostingId, null)).stream()
                .map(Application::getId)
                .toList();
    }

    @Override
    public Long fetchCandidateIdByApplicationId(Long applicationId) {
        return findApplication(applicationId).map(Application::getCandidateId).orElse(0L);
    }

    @Override
    public String fetchApplicationStatus(Long applicationId) {
        return findApplication(applicationId).map(application -> application.getStatus().name()).orElse("");
    }

    @Override
    public Map<String, Long> countApplicationsByStatus(Long jobPostingId) {
        var counts = new LinkedHashMap<String, Long>();
        applicationQueryService.handle(new GetApplicationCountByStatusQuery(jobPostingId))
                .forEach((status, total) -> counts.put(status.name(), total));
        return counts;
    }

    @Override
    public long countPublishedJobPostingsByCompanyId(Long companyId) {
        return companyId == null ? 0L : jobPostingQueryService.handle(new GetPublishedJobPostingCountByCompanyIdQuery(companyId));
    }

    @Override
    public List<Long> fetchJobPostingIdsByCompanyId(Long companyId) {
        return companyId == null ? List.of() : jobPostingQueryService.handle(new GetJobPostingIdsByCompanyIdQuery(companyId));
    }

    @Override
    public Long fetchAverageSecondsToShortlist(List<Long> jobPostingIds) {
        return applicationQueryService.handle(new GetAverageTimeToShortlistQuery(jobPostingIds))
                .map(Duration::getSeconds)
                .orElse(null);
    }

    @Override
    public void markApplicationAsInterviewing(Long applicationId) {
        applicationCommandService.handle(new ChangeApplicationStatusCommand(applicationId, ApplicationStatus.INTERVIEWING));
    }

    @Override
    public void markApplicationAsAssessed(Long applicationId) {
        applicationCommandService.handle(new ChangeApplicationStatusCommand(applicationId, ApplicationStatus.ASSESSED));
    }

    private Optional<JobPosting> findJobPosting(Long jobPostingId) {
        if (jobPostingId == null) {
            return Optional.empty();
        }
        return jobPostingQueryService.handle(new GetJobPostingByIdQuery(jobPostingId, JobPostingViewer.unrestrictedViewer()));
    }

    private Optional<Application> findApplication(Long applicationId) {
        if (applicationId == null) {
            return Optional.empty();
        }
        return applicationQueryService.handle(new GetApplicationByIdQuery(applicationId));
    }
}
