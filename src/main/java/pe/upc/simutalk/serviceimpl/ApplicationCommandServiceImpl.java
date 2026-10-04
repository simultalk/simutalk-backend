package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.ApplicationStatusChangedEvent;
import pe.upc.simutalk.dtos.ChangeApplicationStatusCommand;
import pe.upc.simutalk.dtos.SubmitApplicationCommand;
import pe.upc.simutalk.entities.Application;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.repositories.ApplicationRepository;
import pe.upc.simutalk.repositories.JobPostingRepository;
import pe.upc.simutalk.services.ApplicationCommandService;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class ApplicationCommandServiceImpl implements ApplicationCommandService {

    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Application handle(SubmitApplicationCommand command) {
        var jobPosting = jobPostingRepository.findById(command.jobPostingId())
                .orElseThrow(() -> new ResourceNotFoundException("Job posting", command.jobPostingId()));
        var alreadyApplied = applicationRepository.existsByJobPostingIdAndCandidateId(
                command.jobPostingId(), command.candidateId());
        return applicationRepository.save(
                Application.submit(jobPosting, command.candidateId(), alreadyApplied, Instant.now()));
    }

    @Override
    public Application handle(ChangeApplicationStatusCommand command) {
        var application = applicationRepository.findById(command.applicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", command.applicationId()));
        application.changeStatus(command.status());
        applicationRepository.flush();
        eventPublisher.publishEvent(new ApplicationStatusChangedEvent(application.getId(), application.getJobPostingId(),
                application.getCandidateId(), application.getStatus()));
        return application;
    }
}
