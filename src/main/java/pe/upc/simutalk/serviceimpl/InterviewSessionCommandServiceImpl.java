package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CompleteInterviewSessionCommand;
import pe.upc.simutalk.dtos.CreateInterviewSessionCommand;
import pe.upc.simutalk.dtos.InterviewSessionCompletedEvent;
import pe.upc.simutalk.dtos.RecordAnswerCommand;
import pe.upc.simutalk.dtos.StartInterviewSessionCommand;
import pe.upc.simutalk.entities.Answer;
import pe.upc.simutalk.entities.InterviewSession;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.repositories.InterviewSessionRepository;
import pe.upc.simutalk.repositories.QuestionRepository;
import pe.upc.simutalk.services.InterviewSessionCommandService;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Interview lifecycle. Creating a session moves the application to INTERVIEWING and
 * completing it moves the application to ASSESSED, both through recruitment's ACL and in the
 * same transaction.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class InterviewSessionCommandServiceImpl implements InterviewSessionCommandService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final QuestionRepository questionRepository;
    private final ExternalRecruitmentService externalRecruitmentService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public InterviewSession handle(CreateInterviewSessionCommand command) {
        var application = externalRecruitmentService.fetchApplicationReadyForInterview(command.applicationId());
        if (interviewSessionRepository.existsByApplicationId(command.applicationId())) {
            throw new BusinessRuleViolationException("The application already has an interview session");
        }
        if (questionRepository.countByJobPostingId(application.jobPostingId()) == 0) {
            throw new BusinessRuleViolationException("The job posting has no interview questions yet");
        }
        var session = interviewSessionRepository.save(new InterviewSession(application.applicationId(),
                application.jobPostingId(), application.candidateId(), command.expiresAt(), Instant.now()));
        externalRecruitmentService.markApplicationAsInterviewing(command.applicationId());
        return session;
    }

    @Override
    public InterviewSession handle(StartInterviewSessionCommand command) {
        var session = loadSession(command.interviewSessionId());
        session.start(LocalDate.now(), Instant.now());
        interviewSessionRepository.flush();
        return session;
    }

    @Override
    public Answer handle(RecordAnswerCommand command) {
        var session = loadSession(command.interviewSessionId());
        var question = questionRepository.findById(command.questionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question", command.questionId()));
        var answer = session.recordAnswer(new Answer(command.questionId(), command.transcript(), command.audioUrl(),
                command.durationSeconds(), Instant.now(), command.followUp(), command.parentAnswerId()), question);
        interviewSessionRepository.flush();
        return answer;
    }

    @Override
    public InterviewSession handle(CompleteInterviewSessionCommand command) {
        var session = loadSession(command.interviewSessionId());
        var expectedQuestions = (int) questionRepository.countByJobPostingId(session.getJobPostingId());
        session.complete(expectedQuestions, Instant.now());
        interviewSessionRepository.flush();
        externalRecruitmentService.markApplicationAsAssessed(session.getApplicationId());
        eventPublisher.publishEvent(new InterviewSessionCompletedEvent(session.getId(), session.getApplicationId(),
                session.getJobPostingId(), session.getCandidateId(), session.getFinishedAt()));
        return session;
    }

    private InterviewSession loadSession(Long interviewSessionId) {
        return interviewSessionRepository.findWithAnswersById(interviewSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview session", interviewSessionId));
    }
}
