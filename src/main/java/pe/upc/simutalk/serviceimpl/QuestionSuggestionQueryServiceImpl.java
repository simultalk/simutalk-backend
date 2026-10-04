package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.GetQuestionSuggestionsQuery;
import pe.upc.simutalk.dtos.QuestionSuggestion;
import pe.upc.simutalk.entities.Question;
import pe.upc.simutalk.repositories.QuestionRepository;
import pe.upc.simutalk.services.QuestionSuggestionQueryService;
import pe.upc.simutalk.services.QuestionSuggestionService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Checks through the ACL that questions can still be suggested (DRAFT posting, COMPETENCY criterion of
 * the same posting), reads the posting's and the criterion's text from recruitment and asks the
 * suggestion port. Not transactional on purpose: no database transaction is kept open while the AI
 * provider answers. Nothing is persisted: the recruiter accepts suggestions through the questions endpoint.
 */
@Service
@RequiredArgsConstructor
public class QuestionSuggestionQueryServiceImpl implements QuestionSuggestionQueryService {

    private final QuestionRepository questionRepository;
    private final ExternalRecruitmentService externalRecruitmentService;
    private final QuestionSuggestionService questionSuggestionService;

    @Override
    public List<QuestionSuggestion> handle(GetQuestionSuggestionsQuery query) {
        externalRecruitmentService.ensureQuestionsCanBeSuggested(query.jobPostingId());
        var context = externalRecruitmentService.fetchCompetencyCriterionContext(query.jobPostingId(), query.criterionId());
        var existingQuestions = questionRepository.findAllByJobPostingIdOrderByPositionAscIdAsc(query.jobPostingId())
                .stream().map(Question::getStatement).toList();
        return questionSuggestionService.suggest(context.jobTitle(), context.jobDescription(), context.criterionName(),
                context.criterionDescription(), existingQuestions);
    }
}
