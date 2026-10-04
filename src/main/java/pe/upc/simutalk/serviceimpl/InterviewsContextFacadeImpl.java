package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CountQuestionsByCriterionIdQuery;
import pe.upc.simutalk.dtos.GetInterviewSessionByApplicationIdQuery;
import pe.upc.simutalk.dtos.GetInterviewSessionByIdQuery;
import pe.upc.simutalk.dtos.GetQuestionsByJobPostingIdQuery;
import pe.upc.simutalk.dtos.InterviewAnswerView;
import pe.upc.simutalk.entities.InterviewSession;
import pe.upc.simutalk.entities.Question;
import pe.upc.simutalk.services.InterviewSessionQueryService;
import pe.upc.simutalk.services.InterviewsContextFacade;
import pe.upc.simutalk.services.QuestionQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * interviews' implementation of the {@link InterviewsContextFacade} contract published in shared.
 */
@Service
@RequiredArgsConstructor
public class InterviewsContextFacadeImpl implements InterviewsContextFacade {

    private final QuestionQueryService questionQueryService;
    private final InterviewSessionQueryService interviewSessionQueryService;

    @Override
    public long countQuestionsByCriterionId(Long criterionId) {
        return questionQueryService.handle(new CountQuestionsByCriterionIdQuery(criterionId));
    }

    @Override
    public String fetchSessionStatus(Long interviewSessionId) {
        return findSession(interviewSessionId).map(session -> session.getStatus().name()).orElse("");
    }

    @Override
    public Long fetchSessionIdByApplicationId(Long applicationId) {
        if (applicationId == null) {
            return 0L;
        }
        return interviewSessionQueryService.handle(new GetInterviewSessionByApplicationIdQuery(applicationId))
                .map(InterviewSession::getId)
                .orElse(0L);
    }

    @Override
    public Long fetchApplicationIdBySessionId(Long interviewSessionId) {
        return findSession(interviewSessionId).map(InterviewSession::getApplicationId).orElse(0L);
    }

    @Override
    public Long fetchJobPostingIdBySessionId(Long interviewSessionId) {
        return findSession(interviewSessionId).map(InterviewSession::getJobPostingId).orElse(0L);
    }

    @Override
    public Long fetchCandidateIdBySessionId(Long interviewSessionId) {
        return findSession(interviewSessionId).map(InterviewSession::getCandidateId).orElse(0L);
    }

    @Override
    public List<InterviewAnswerView> fetchAnswers(Long interviewSessionId) {
        return findSession(interviewSessionId)
                .map(session -> {
                    Map<Long, Question> script = questionQueryService
                            .handle(new GetQuestionsByJobPostingIdQuery(session.getJobPostingId())).stream()
                            .collect(Collectors.toMap(Question::getId, Function.identity()));
                    return session.getAnswers().stream()
                            .filter(answer -> script.containsKey(answer.getQuestionId()))
                            .map(answer -> new InterviewAnswerView(answer.getId(), answer.getQuestionId(),
                                    script.get(answer.getQuestionId()).getCriterionId(), answer.getTranscript(),
                                    answer.isFollowUp()))
                            .toList();
                })
                .orElse(List.of());
    }

    private Optional<InterviewSession> findSession(Long interviewSessionId) {
        if (interviewSessionId == null) {
            return Optional.empty();
        }
        return interviewSessionQueryService.handle(new GetInterviewSessionByIdQuery(interviewSessionId));
    }
}
