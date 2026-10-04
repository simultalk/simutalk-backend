package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.AddEvaluationCriterionCommand;
import pe.upc.simutalk.dtos.ChangeJobPostingStatusCommand;
import pe.upc.simutalk.dtos.CreateJobPostingCommand;
import pe.upc.simutalk.dtos.DeleteJobPostingCommand;
import pe.upc.simutalk.dtos.RemoveEvaluationCriterionCommand;
import pe.upc.simutalk.dtos.UpdateEvaluationCriterionCommand;
import pe.upc.simutalk.dtos.UpdateJobPostingCommand;
import pe.upc.simutalk.entities.EvaluationCriterion;
import pe.upc.simutalk.entities.JobPosting;
import pe.upc.simutalk.entities.Weight;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.repositories.JobPostingRepository;
import pe.upc.simutalk.services.JobPostingCommandService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the aggregate, delegates the change to it and persists the result.
 * No business rule lives here.
 * <p>
 * Loaded aggregates are managed by the persistence context, so changes are written
 * on {@code flush()}. Calling {@code save()} on them would {@code merge} and cascade
 * copies of newly added criteria, leaving the returned instances without id.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class JobPostingCommandServiceImpl implements JobPostingCommandService {

    private final JobPostingRepository jobPostingRepository;
    private final ExternalInterviewsService externalInterviewsService;

    @Override
    public JobPosting handle(CreateJobPostingCommand command) {
        return jobPostingRepository.save(new JobPosting(command));
    }

    @Override
    public JobPosting handle(UpdateJobPostingCommand command) {
        var jobPosting = loadJobPosting(command.jobPostingId());
        jobPosting.updateDetails(command.title(), command.description(), command.closingDate(),
                command.anonymizedScreening());
        jobPostingRepository.flush();
        return jobPosting;
    }

    @Override
    public void handle(DeleteJobPostingCommand command) {
        var jobPosting = loadJobPosting(command.jobPostingId());
        jobPosting.ensureCanBeDeleted();
        jobPostingRepository.delete(jobPosting);
    }

    @Override
    public JobPosting handle(ChangeJobPostingStatusCommand command) {
        var jobPosting = loadJobPosting(command.jobPostingId());
        jobPosting.changeStatus(command.status(), externalInterviewsService);
        jobPostingRepository.flush();
        return jobPosting;
    }

    @Override
    public EvaluationCriterion handle(AddEvaluationCriterionCommand command) {
        var jobPosting = loadJobPosting(command.jobPostingId());
        var criterion = jobPosting.addCriterion(command.name(), command.description(), new Weight(command.weight()),
                command.criterionType(), command.certificationName(), command.mandatory(), command.origin());
        jobPostingRepository.flush();
        return criterion;
    }

    @Override
    public EvaluationCriterion handle(UpdateEvaluationCriterionCommand command) {
        var jobPosting = loadJobPosting(command.jobPostingId());
        var criterion = jobPosting.updateCriterion(command.criterionId(), command.name(), command.description(),
                new Weight(command.weight()), command.criterionType(), command.certificationName(), command.mandatory());
        jobPostingRepository.flush();
        return criterion;
    }

    @Override
    public void handle(RemoveEvaluationCriterionCommand command) {
        var jobPosting = loadJobPosting(command.jobPostingId());
        jobPosting.removeCriterion(command.criterionId());
    }

    private JobPosting loadJobPosting(Long jobPostingId) {
        return jobPostingRepository.findWithCriteriaById(jobPostingId)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting", jobPostingId));
    }
}
