package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.GetAnswersByInterviewSessionIdQuery;
import pe.upc.simutalk.dtos.GetInterviewSessionByApplicationIdQuery;
import pe.upc.simutalk.dtos.GetInterviewSessionByIdQuery;
import pe.upc.simutalk.dtos.HasInterviewSessionInProgressQuery;
import pe.upc.simutalk.entities.Answer;
import pe.upc.simutalk.entities.InterviewSession;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionQueryService {

    Optional<InterviewSession> handle(GetInterviewSessionByIdQuery query);

    Optional<InterviewSession> handle(GetInterviewSessionByApplicationIdQuery query);

    /**
     * @throws ResourceNotFoundException if the session does not exist
     */
    List<Answer> handle(GetAnswersByInterviewSessionIdQuery query);

    /** Whether the candidate has an IN_PROGRESS session for the job posting. */
    boolean handle(HasInterviewSessionInProgressQuery query);
}
