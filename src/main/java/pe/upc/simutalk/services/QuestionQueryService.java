package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CountQuestionsByCriterionIdQuery;
import pe.upc.simutalk.dtos.GetQuestionByIdQuery;
import pe.upc.simutalk.dtos.GetQuestionsByJobPostingIdQuery;
import pe.upc.simutalk.entities.Question;

import java.util.List;
import java.util.Optional;

public interface QuestionQueryService {

    /** The script ordered by position. */
    List<Question> handle(GetQuestionsByJobPostingIdQuery query);

    Optional<Question> handle(GetQuestionByIdQuery query);

    long handle(CountQuestionsByCriterionIdQuery query);
}
