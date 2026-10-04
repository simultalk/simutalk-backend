package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.services.AnswerScoringService.CriterionFeedback;

import pe.upc.simutalk.dtos.CandidatePersonalData;
import pe.upc.simutalk.dtos.ComputeAssessmentCommand;
import pe.upc.simutalk.dtos.CriterionView;
import pe.upc.simutalk.dtos.InterviewAnswerView;
import pe.upc.simutalk.dtos.InterviewSessionSnapshot;
import pe.upc.simutalk.entities.Assessment;
import pe.upc.simutalk.entities.CriterionScore;
import pe.upc.simutalk.entities.Evidence;
import pe.upc.simutalk.entities.IntegrityFlag;
import pe.upc.simutalk.enums.CriterionKind;
import pe.upc.simutalk.enums.FlagSeverity;
import pe.upc.simutalk.enums.IntegrityFlagType;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.repositories.AssessmentRepository;
import pe.upc.simutalk.services.AnswerScoringService;
import pe.upc.simutalk.services.AssessmentCommandService;
import pe.upc.simutalk.services.CertificationScoringPolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scores a completed interview:
 * <ol>
 *   <li>each answer is anonymized and sent, with its criterion only, to the scoring port;</li>
 *   <li>the excerpt returned is mapped back and anchored in the ORIGINAL transcript as evidence;</li>
 *   <li>CERTIFICATION criteria are scored from the candidate's verified certifications;</li>
 *   <li>the Assessment aggregate checks its rules and computes the weighted score.</li>
 * </ol>
 * Not transactional on purpose: the provider may be slow and no database transaction is kept
 * open while waiting for it. The aggregate is saved at the end in its own transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentCommandServiceImpl implements AssessmentCommandService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentExternalContextsService externalContextsService;
    private final TranscriptAnonymizer transcriptAnonymizer;
    private final AnswerScoringService answerScoringService;

    @Override
    public Assessment handle(ComputeAssessmentCommand command) {
        var session = externalContextsService.fetchSession(command.interviewSessionId());
        if (!session.isCompleted()) {
            throw new BusinessRuleViolationException(
                    "Only a COMPLETED interview can be assessed (current: %s)".formatted(session.status()));
        }
        if (assessmentRepository.existsByInterviewSessionId(session.interviewSessionId())) {
            throw new BusinessRuleViolationException("This interview has already been assessed");
        }
        var criteria = externalContextsService.fetchCriteria(session.jobPostingId());
        var answers = externalContextsService.fetchAnswers(session.interviewSessionId());
        var candidate = externalContextsService.fetchCandidatePersonalData(session.candidateId()).orElse(null);

        var aiSuspicions = new int[]{0};
        var scores = new ArrayList<CriterionScore>();
        var anonymizedExcerpts = new HashMap<Long, String>();
        for (var criterion : criteria) {
            scores.add(criterion.isCompetency()
                    ? scoreCompetency(criterion, answers, candidate, aiSuspicions, anonymizedExcerpts)
                    : scoreCertification(criterion, session.candidateId()));
        }
        var flags = integrityFlags(session, answers.size(), aiSuspicions[0]);
        var assessment = Assessment.calculate(session, scores, flags, answerScoringService.engineVersion(), Instant.now());
        recordFeedback(assessment, anonymizedExcerpts);
        return assessmentRepository.save(assessment);
    }

    /**
     * Candidate feedback from the scores and the ANONYMIZED excerpts only: the ranking, other candidates and
     * the integrity flags are not even passed to the port.
     */
    private void recordFeedback(Assessment assessment, Map<Long, String> anonymizedExcerpts) {
        var inputs = assessment.getCriterionScores().stream()
                .map(score -> new CriterionFeedback(score.getCriterionName(), score.getCriterionKind(), score.getScore(),
                        score.getWeightApplied(), anonymizedExcerpts.get(score.getCriterionId())))
                .toList();
        var feedback = answerScoringService.summarizeFeedback(assessment.getWeightedScore(), inputs);
        if (feedback != null && !feedback.isBlank()) {
            assessment.recordFeedback(feedback.length() > Assessment.FEEDBACK_MAX_LENGTH
                    ? feedback.substring(0, Assessment.FEEDBACK_MAX_LENGTH) : feedback);
        }
    }

    private CriterionScore scoreCompetency(CriterionView criterion, List<InterviewAnswerView> answers,
                                           CandidatePersonalData candidate, int[] aiSuspicions,
                                           Map<Long, String> anonymizedExcerpts) {
        var evidences = new ArrayList<Evidence>();
        var scoreSum = BigDecimal.ZERO;
        var confidenceSum = BigDecimal.ZERO;
        for (var answer : answers.stream().filter(answer -> criterion.criterionId().equals(answer.criterionId())).toList()) {
            var anonymized = transcriptAnonymizer.anonymize(answer.transcript(), candidate);
            var result = answerScoringService.score(anonymized.text(), criterion.name(), criterion.description());
            if (!result.isAvailable()) {
                continue;
            }
            var start = anonymized.toOriginalOffset(result.startOffset(), false);
            var end = anonymized.toOriginalOffset(result.endOffset(), true);
            if (start >= end) {
                continue;
            }
            evidences.add(new Evidence(answer.answerId(), answer.transcript().substring(start, end), start, end));
            anonymizedExcerpts.putIfAbsent(criterion.criterionId(), result.excerpt());
            scoreSum = scoreSum.add(result.score());
            confidenceSum = confidenceSum.add(result.confidence());
            if (result.aiGeneratedSuspicion()) {
                aiSuspicions[0]++;
            }
        }
        if (evidences.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No evidence could be obtained for criterion '%s'; the assessment was not computed. Try again later."
                            .formatted(criterion.name()));
        }
        var count = BigDecimal.valueOf(evidences.size());
        return new CriterionScore(criterion.criterionId(), criterion.name(), CriterionKind.COMPETENCY,
                scoreSum.divide(count, 1, RoundingMode.HALF_UP), criterion.weight(),
                confidenceSum.divide(count, 2, RoundingMode.HALF_UP), evidences);
    }

    private CriterionScore scoreCertification(CriterionView criterion, Long candidateId) {
        var verified = externalContextsService.fetchVerifiedCertificationCount(candidateId);
        return new CriterionScore(criterion.criterionId(), criterion.name(), CriterionKind.CERTIFICATION,
                CertificationScoringPolicy.scoreFor(verified), criterion.weight(), BigDecimal.ONE, List.of());
    }

    private List<IntegrityFlag> integrityFlags(InterviewSessionSnapshot session, int answerCount, int aiSuspicions) {
        var flags = new ArrayList<IntegrityFlag>();
        var now = Instant.now();
        if (aiSuspicions > 0) {
            var severity = aiSuspicions * 2 >= answerCount ? FlagSeverity.HIGH : FlagSeverity.MEDIUM;
            flags.add(new IntegrityFlag(IntegrityFlagType.AI_GENERATED_CONTENT, severity,
                    "%d de %d respuestas parecen redactadas por un asistente de IA".formatted(aiSuspicions, answerCount), now));
        }
        var rejected = externalContextsService.fetchRejectedCertificationCount(session.candidateId());
        if (rejected > 0) {
            flags.add(new IntegrityFlag(IntegrityFlagType.CV_INCONSISTENCY, FlagSeverity.MEDIUM,
                    "%d certificación(es) declarada(s) no coincidieron con el emisor".formatted(rejected), now));
        }
        return flags;
    }
}
