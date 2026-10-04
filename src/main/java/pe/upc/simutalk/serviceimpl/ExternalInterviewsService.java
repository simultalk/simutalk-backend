package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.services.InterviewQuestionCounter;
import pe.upc.simutalk.services.InterviewsContextFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Anti-corruption layer from recruitment to interviews, through the InterviewsContextFacade
 * contract in shared.
 */
@Service
@RequiredArgsConstructor
public class ExternalInterviewsService implements InterviewQuestionCounter {

    private final InterviewsContextFacade interviewsContextFacade;

    @Override
    public long countQuestionsByCriterionId(Long criterionId) {
        return interviewsContextFacade.countQuestionsByCriterionId(criterionId);
    }
}
