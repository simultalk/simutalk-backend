package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.AddEvaluationCriterionCommand;
import pe.upc.simutalk.dtos.ChangeJobPostingStatusCommand;
import pe.upc.simutalk.dtos.CreateJobPostingCommand;
import pe.upc.simutalk.dtos.GetJobPostingByIdQuery;
import pe.upc.simutalk.dtos.GetJobPostingIdsByCompanyIdQuery;
import pe.upc.simutalk.dtos.JobPostingViewer;
import pe.upc.simutalk.dtos.SubmitApplicationCommand;
import pe.upc.simutalk.enums.CriterionType;
import pe.upc.simutalk.enums.JobPostingStatus;
import pe.upc.simutalk.services.ApplicationCommandService;
import pe.upc.simutalk.services.IamContextFacade;
import pe.upc.simutalk.services.JobPostingCommandService;
import pe.upc.simutalk.services.JobPostingQueryService;
import pe.upc.simutalk.services.ProfilesContextFacade;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

/**
 * Recruitment part of the demo data ({@code app.seed-demo-data=true}). The demo seeding runs as
 * one sequence of {@link ApplicationReadyEvent} listeners ordered with {@link Order}, one or two
 * steps per context, each in its own transaction:
 * <ol>
 *   <li>100 profiles: demo company and candidates;</li>
 *   <li>200 recruitment ({@link #draftJobPosting}): DRAFT job posting for Consultora Andina with its criteria;</li>
 *   <li>300 interviews: interview script (publishing requires questions for every COMPETENCY criterion);</li>
 *   <li>400 recruitment ({@link #publishAndApply}): publishes the job posting and makes the six demo
 *       candidates apply (RECEIVED);</li>
 *   <li>500 interviews: interview sessions (INTERVIEWING / ASSESSED);</li>
 *   <li>600 assessment: scores the completed interviews.</li>
 * </ol>
 * Each step finds what the previous one left through the shared facades; no other context's
 * classes are imported. Each step checks its own precondition, so the sequence is idempotent.
 */
@Slf4j
@Service
public class RecruitmentDemoDataSeeder {

    private static final String COMPANY_USERNAME = "consultora.andina";

    /** Demo candidates created by the profiles seeder, in application order. */
    private static final List<String> APPLICATION_ORDER = List.of(
            "rosa.quispe", "jorge.huaman", "lucia.flores", "miguel.condori", "carmen.ramos", "diego.salazar");

    private final boolean enabled;
    private final IamContextFacade iamContextFacade;
    private final ProfilesContextFacade profilesContextFacade;
    private final JobPostingCommandService jobPostingCommandService;
    private final JobPostingQueryService jobPostingQueryService;
    private final ApplicationCommandService applicationCommandService;
    private final TransactionTemplate transactionTemplate;

    public RecruitmentDemoDataSeeder(@Value("${app.seed-demo-data:false}") boolean enabled,
                                     IamContextFacade iamContextFacade,
                                     ProfilesContextFacade profilesContextFacade,
                                     JobPostingCommandService jobPostingCommandService,
                                     JobPostingQueryService jobPostingQueryService,
                                     ApplicationCommandService applicationCommandService,
                                     TransactionTemplate transactionTemplate) {
        this.enabled = enabled;
        this.iamContextFacade = iamContextFacade;
        this.profilesContextFacade = profilesContextFacade;
        this.jobPostingCommandService = jobPostingCommandService;
        this.jobPostingQueryService = jobPostingQueryService;
        this.applicationCommandService = applicationCommandService;
        this.transactionTemplate = transactionTemplate;
    }

    /** Step 200: only when the demo company has no job postings yet. */
    @EventListener(ApplicationReadyEvent.class)
    @Order(200)
    public void draftJobPosting(ApplicationReadyEvent event) {
        // Demo data must never keep the application from starting.
        try {
            seedDraftJobPosting();
        } catch (RuntimeException ex) {
            log.warn("Recruitment demo data: the DRAFT demo job posting could not be created; the application starts without it", ex);
        }
    }

    private void seedDraftJobPosting() {
        if (!enabled) {
            return;
        }
        var companyId = profilesContextFacade.fetchCompanyIdByUserId(userIdOf(COMPANY_USERNAME));
        if (companyId == 0L) {
            log.warn("Recruitment demo data skipped: demo company '{}' not found", COMPANY_USERNAME);
            return;
        }
        if (!jobPostingQueryService.handle(new GetJobPostingIdsByCompanyIdQuery(companyId)).isEmpty()) {
            log.info("Recruitment demo data skipped: the demo company already has job postings");
            return;
        }
        transactionTemplate.executeWithoutResult(status -> draft(companyId));
        log.info("Recruitment demo data: DRAFT job posting created");
    }

    /** Step 400: only while the demo job posting is still in DRAFT. */
    @EventListener(ApplicationReadyEvent.class)
    @Order(400)
    public void publishAndApply(ApplicationReadyEvent event) {
        // Demo data must never keep the application from starting.
        try {
            seedPublishedJobPosting();
        } catch (RuntimeException ex) {
            log.warn("Recruitment demo data: the demo job posting could not be published or the demo applications submitted; the application starts without them", ex);
        }
    }

    private void seedPublishedJobPosting() {
        if (!enabled) {
            return;
        }
        var companyId = profilesContextFacade.fetchCompanyIdByUserId(userIdOf(COMPANY_USERNAME));
        var jobPostingIds = companyId == 0L ? List.<Long>of()
                : jobPostingQueryService.handle(new GetJobPostingIdsByCompanyIdQuery(companyId));
        if (jobPostingIds.isEmpty()) {
            return;
        }
        var jobPostingId = jobPostingIds.getFirst();
        var isDraft = jobPostingQueryService
                .handle(new GetJobPostingByIdQuery(jobPostingId, JobPostingViewer.unrestrictedViewer()))
                .map(jobPosting -> jobPosting.isDraft())
                .orElse(false);
        if (!isDraft) {
            return;
        }
        var submitted = transactionTemplate.execute(status -> publishAndApply(jobPostingId));
        log.info("Recruitment demo data seeded: 1 published job posting and {} applications", submitted);
    }

    private void draft(Long companyId) {
        var jobPosting = jobPostingCommandService.handle(new CreateJobPostingCommand(
                "Analista de Datos Junior",
                "Análisis de datos comerciales con SQL, Excel y Python; elaboración de reportes para clientes.",
                companyId, LocalDate.now().plusDays(60), true));
        var jobPostingId = jobPosting.getId();
        // Created in this order on purpose: interviews reads the COMPETENCY criteria by id.
        jobPostingCommandService.handle(new AddEvaluationCriterionCommand(jobPostingId, "Pensamiento analítico",
                "Descompone problemas de negocio y los resuelve con datos.", 40, CriterionType.COMPETENCY, null, false));
        jobPostingCommandService.handle(new AddEvaluationCriterionCommand(jobPostingId, "Comunicación efectiva",
                "Explica hallazgos a públicos no técnicos con claridad.", 30, CriterionType.COMPETENCY, null, false));
        jobPostingCommandService.handle(new AddEvaluationCriterionCommand(jobPostingId, "Certificación en análisis de datos",
                "Certificación reconocida en análisis de datos.", 30, CriterionType.CERTIFICATION,
                "Google Data Analytics", false));
    }

    private int publishAndApply(Long jobPostingId) {
        jobPostingCommandService.handle(new ChangeJobPostingStatusCommand(jobPostingId, JobPostingStatus.PUBLISHED));
        var submitted = 0;
        for (var username : APPLICATION_ORDER) {
            var candidateId = profilesContextFacade.fetchCandidateIdByUserId(userIdOf(username));
            if (candidateId == 0L) {
                log.warn("Demo candidate '{}' has no profile; skipped", username);
                continue;
            }
            applicationCommandService.handle(new SubmitApplicationCommand(jobPostingId, candidateId));
            submitted++;
        }
        return submitted;
    }

    private Long userIdOf(String username) {
        var userId = iamContextFacade.fetchUserIdByUsername(username);
        return userId == null ? 0L : userId;
    }
}
