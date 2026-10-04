package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.ComputeAssessmentCommand;
import pe.upc.simutalk.repositories.AssessmentRepository;
import pe.upc.simutalk.services.AnswerScoringService;
import pe.upc.simutalk.services.AssessmentCommandService;
import pe.upc.simutalk.services.IamContextFacade;
import pe.upc.simutalk.services.InterviewsContextFacade;
import pe.upc.simutalk.services.ProfilesContextFacade;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Assessment part of the demo data ({@code app.seed-demo-data=true}), last step (600) of the ordered
 * demo sequence described in recruitment's seeder: scores the COMPLETED interviews of the demo job
 * posting through the same command service as the API, so the evidences are real fragments of the
 * demo transcripts. Skipped once the job posting has any assessment, and only runs with the mock
 * scoring engine, so demo seeding never calls an external provider.
 */
@Slf4j
@Service
public class AssessmentDemoDataSeeder {

    private static final String COMPANY_USERNAME = "consultora.andina";

    private final boolean enabled;
    private final AssessmentCommandService assessmentCommandService;
    private final AnswerScoringService answerScoringService;
    private final AssessmentRepository assessmentRepository;
    private final IamContextFacade iamContextFacade;
    private final ProfilesContextFacade profilesContextFacade;
    private final RecruitmentContextFacade recruitmentContextFacade;
    private final InterviewsContextFacade interviewsContextFacade;

    public AssessmentDemoDataSeeder(@Value("${app.seed-demo-data:false}") boolean enabled,
                                    AssessmentCommandService assessmentCommandService,
                                    AnswerScoringService answerScoringService,
                                    AssessmentRepository assessmentRepository,
                                    IamContextFacade iamContextFacade,
                                    ProfilesContextFacade profilesContextFacade,
                                    RecruitmentContextFacade recruitmentContextFacade,
                                    InterviewsContextFacade interviewsContextFacade) {
        this.enabled = enabled;
        this.assessmentCommandService = assessmentCommandService;
        this.answerScoringService = answerScoringService;
        this.assessmentRepository = assessmentRepository;
        this.iamContextFacade = iamContextFacade;
        this.profilesContextFacade = profilesContextFacade;
        this.recruitmentContextFacade = recruitmentContextFacade;
        this.interviewsContextFacade = interviewsContextFacade;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(600)
    public void on(ApplicationReadyEvent event) {
        // Demo data must never keep the application from starting.
        try {
            seedAssessments();
        } catch (RuntimeException ex) {
            log.warn("Assessment demo data: the completed demo interviews could not be assessed; the application starts without those assessments", ex);
        }
    }

    private void seedAssessments() {
        if (!enabled) {
            return;
        }
        var userId = iamContextFacade.fetchUserIdByUsername(COMPANY_USERNAME);
        var companyId = userId == null ? 0L : profilesContextFacade.fetchCompanyIdByUserId(userId);
        var jobPostingIds = companyId == 0L ? List.<Long>of() : recruitmentContextFacade.fetchJobPostingIdsByCompanyId(companyId);
        if (jobPostingIds.isEmpty() || assessmentRepository.countByJobPostingIdIn(List.of(jobPostingIds.getFirst())) > 0) {
            return;
        }
        var completedSessionIds = recruitmentContextFacade.fetchApplicationIds(jobPostingIds.getFirst()).stream()
                .map(interviewsContextFacade::fetchSessionIdByApplicationId)
                .filter(sessionId -> "COMPLETED".equals(interviewsContextFacade.fetchSessionStatus(sessionId)))
                .toList();
        if (completedSessionIds.isEmpty()) {
            return;
        }
        if (!answerScoringService.engineVersion().startsWith("mock")) {
            log.info("Assessment demo data skipped: scoring engine is {}, not mock", answerScoringService.engineVersion());
            return;
        }
        completedSessionIds.forEach(sessionId -> assessmentCommandService.handle(new ComputeAssessmentCommand(sessionId)));
        log.info("Assessment demo data: {} interviews assessed", completedSessionIds.size());
    }
}
