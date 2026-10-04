package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CountQuestionsByCriterionIdQuery;
import pe.upc.simutalk.dtos.GetQuestionByIdQuery;
import pe.upc.simutalk.dtos.GetQuestionsByJobPostingIdQuery;
import pe.upc.simutalk.entities.Question;
import pe.upc.simutalk.repositories.QuestionRepository;
import pe.upc.simutalk.services.QuestionQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionQueryServiceImpl implements QuestionQueryService {

    private final QuestionRepository questionRepository;

    @Override
    public List<Question> handle(GetQuestionsByJobPostingIdQuery query) {
        return questionRepository.findAllByJobPostingIdOrderByPositionAscIdAsc(query.jobPostingId());
    }

    @Override
    public Optional<Question> handle(GetQuestionByIdQuery query) {
        return questionRepository.findById(query.questionId());
    }

    @Override
    public long handle(CountQuestionsByCriterionIdQuery query) {
        return query.criterionId() == null ? 0L : questionRepository.countByCriterionId(query.criterionId());
    }
}
