package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.GetAnswersByInterviewSessionIdQuery;
import pe.upc.simutalk.dtos.GetInterviewSessionByApplicationIdQuery;
import pe.upc.simutalk.dtos.GetInterviewSessionByIdQuery;
import pe.upc.simutalk.dtos.HasInterviewSessionInProgressQuery;
import pe.upc.simutalk.entities.Answer;
import pe.upc.simutalk.entities.InterviewSession;
import pe.upc.simutalk.enums.InterviewSessionStatus;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.repositories.InterviewSessionRepository;
import pe.upc.simutalk.services.InterviewSessionQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InterviewSessionQueryServiceImpl implements InterviewSessionQueryService {

    private final InterviewSessionRepository interviewSessionRepository;

    @Override
    public Optional<InterviewSession> handle(GetInterviewSessionByIdQuery query) {
        return interviewSessionRepository.findWithAnswersById(query.interviewSessionId());
    }

    @Override
    public Optional<InterviewSession> handle(GetInterviewSessionByApplicationIdQuery query) {
        return interviewSessionRepository.findByApplicationId(query.applicationId());
    }

    @Override
    public List<Answer> handle(GetAnswersByInterviewSessionIdQuery query) {
        return interviewSessionRepository.findWithAnswersById(query.interviewSessionId())
                .map(InterviewSession::getAnswers)
                .orElseThrow(() -> new ResourceNotFoundException("Interview session", query.interviewSessionId()));
    }

    @Override
    public boolean handle(HasInterviewSessionInProgressQuery query) {
        return query.jobPostingId() != null && query.candidateId() != null
                && interviewSessionRepository.existsByJobPostingIdAndCandidateIdAndStatus(
                query.jobPostingId(), query.candidateId(), InterviewSessionStatus.IN_PROGRESS);
    }
}
