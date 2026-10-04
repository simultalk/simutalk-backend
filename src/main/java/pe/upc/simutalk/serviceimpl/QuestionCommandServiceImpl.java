package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CreateQuestionCommand;
import pe.upc.simutalk.dtos.DeleteQuestionCommand;
import pe.upc.simutalk.dtos.ReorderQuestionsCommand;
import pe.upc.simutalk.dtos.UpdateQuestionCommand;
import pe.upc.simutalk.entities.Question;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.repositories.QuestionRepository;
import pe.upc.simutalk.services.QuestionCommandService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/**
 * Script edition. The rules that need recruitment (DRAFT posting, COMPETENCY criterion of the
 * same posting) are checked here through the ACL; the question's own rules live in the
 * aggregate. Positions are kept consecutive (1..n) within a job posting.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class QuestionCommandServiceImpl implements QuestionCommandService {

    private final QuestionRepository questionRepository;
    private final ExternalRecruitmentService externalRecruitmentService;

    @Override
    public Question handle(CreateQuestionCommand command) {
        externalRecruitmentService.ensureScriptIsEditable(command.jobPostingId());
        externalRecruitmentService.ensureCompetencyCriterion(command.jobPostingId(), command.criterionId());
        var position = (int) questionRepository.countByJobPostingId(command.jobPostingId()) + 1;
        return questionRepository.save(new Question(command.jobPostingId(), command.criterionId(), command.statement(),
                command.maxDurationSeconds(), position, command.origin(), command.allowsFollowUp()));
    }

    @Override
    public Question handle(UpdateQuestionCommand command) {
        var question = loadQuestion(command.jobPostingId(), command.questionId());
        externalRecruitmentService.ensureScriptIsEditable(command.jobPostingId());
        externalRecruitmentService.ensureCompetencyCriterion(command.jobPostingId(), command.criterionId());
        question.updateDetails(command.criterionId(), command.statement(), command.maxDurationSeconds(),
                command.origin(), command.allowsFollowUp());
        questionRepository.flush();
        return question;
    }

    @Override
    public void handle(DeleteQuestionCommand command) {
        var question = loadQuestion(command.jobPostingId(), command.questionId());
        externalRecruitmentService.ensureScriptIsEditable(command.jobPostingId());
        questionRepository.delete(question);
        var remaining = questionRepository.findAllByJobPostingIdOrderByPositionAscIdAsc(command.jobPostingId()).stream()
                .filter(other -> !other.getId().equals(question.getId()))
                .toList();
        renumber(remaining);
        questionRepository.flush();
    }

    @Override
    public List<Question> handle(ReorderQuestionsCommand command) {
        externalRecruitmentService.ensureScriptIsEditable(command.jobPostingId());
        var script = questionRepository.findAllByJobPostingIdOrderByPositionAscIdAsc(command.jobPostingId());
        var scriptIds = script.stream().map(Question::getId).toList();
        var requested = command.orderedIds();
        if (requested.size() != scriptIds.size() || !new HashSet<>(requested).equals(new HashSet<>(scriptIds))) {
            throw new BusinessRuleViolationException(
                    "The new order must list every question of the script exactly once: %s".formatted(scriptIds));
        }
        var ordered = requested.stream()
                .map(id -> script.stream().filter(question -> question.getId().equals(id)).findFirst().orElseThrow())
                .toList();
        renumber(ordered);
        questionRepository.flush();
        return ordered;
    }

    private void renumber(List<Question> orderedQuestions) {
        for (var index = 0; index < orderedQuestions.size(); index++) {
            orderedQuestions.get(index).moveTo(index + 1);
        }
    }

    private Question loadQuestion(Long jobPostingId, Long questionId) {
        return questionRepository.findById(questionId)
                .filter(question -> question.belongsTo(jobPostingId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question %s not found in job posting %s".formatted(questionId, jobPostingId)));
    }
}
