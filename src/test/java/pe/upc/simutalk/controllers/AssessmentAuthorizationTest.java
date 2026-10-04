package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.AssessmentResource;
import pe.upc.simutalk.dtos.CandidateAssessmentResource;
import pe.upc.simutalk.dtos.GetAssessmentByInterviewSessionIdQuery;
import pe.upc.simutalk.dtos.InterviewSessionSnapshot;
import pe.upc.simutalk.entities.Assessment;
import pe.upc.simutalk.entities.CriterionScore;
import pe.upc.simutalk.entities.Evidence;
import pe.upc.simutalk.entities.IntegrityFlag;
import pe.upc.simutalk.enums.CriterionKind;
import pe.upc.simutalk.enums.FlagSeverity;
import pe.upc.simutalk.enums.IntegrityFlagType;
import pe.upc.simutalk.securities.AssessmentAccessPolicy;
import pe.upc.simutalk.services.AssessmentCommandService;
import pe.upc.simutalk.services.AssessmentQueryService;
import pe.upc.simutalk.services.IamContextFacade;
import pe.upc.simutalk.services.InterviewsContextFacade;
import pe.upc.simutalk.services.ProfilesContextFacade;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * US-22: evaluates the real {@code @PreAuthorize} expression of GET /interview-sessions/{id}/assessment with
 * method security enabled. Session 1 is candidate 7's (user rosa), on job posting 10 of company 1 (recruiter.a).
 */
@SpringJUnitWebConfig(AssessmentAuthorizationTest.Config.class)
class AssessmentAuthorizationTest {

    private static final long SESSION_ID = 1L;

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean AssessmentCommandService assessmentCommandService() { return mock(AssessmentCommandService.class); }
        @Bean AssessmentQueryService assessmentQueryService() { return mock(AssessmentQueryService.class); }
        @Bean IamContextFacade iamContextFacade() { return mock(IamContextFacade.class); }
        @Bean ProfilesContextFacade profilesContextFacade() { return mock(ProfilesContextFacade.class); }
        @Bean RecruitmentContextFacade recruitmentContextFacade() { return mock(RecruitmentContextFacade.class); }
        @Bean InterviewsContextFacade interviewsContextFacade() { return mock(InterviewsContextFacade.class); }

        @Bean(name = "assessmentAccess")
        AssessmentAccessPolicy assessmentAccess(IamContextFacade iam, ProfilesContextFacade profiles,
                                                RecruitmentContextFacade recruitment, InterviewsContextFacade interviews,
                                                AssessmentQueryService queries) {
            return new AssessmentAccessPolicy(iam, profiles, recruitment, interviews, queries);
        }

        @Bean AssessmentController assessmentsController(AssessmentCommandService commands, AssessmentQueryService queries,
                                                          AssessmentAccessPolicy policy) {
            return new AssessmentController(commands, queries, policy);
        }
    }

    @Autowired AssessmentController controller;
    @Autowired AssessmentQueryService queryService;
    @Autowired IamContextFacade iam;
    @Autowired ProfilesContextFacade profiles;
    @Autowired RecruitmentContextFacade recruitment;
    @Autowired InterviewsContextFacade interviews;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        reset(queryService, iam, profiles, recruitment, interviews);
        var assessment = Assessment.calculate(new InterviewSessionSnapshot(SESSION_ID, 50L, 10L, 7L, "COMPLETED"),
                List.of(new CriterionScore(1L, "Pensamiento analítico", CriterionKind.COMPETENCY, new BigDecimal("8.0"), 100,
                        new BigDecimal("0.80"), List.of(new Evidence(300L, "separaría la caída", 0, 18)))),
                List.of(new IntegrityFlag(IntegrityFlagType.AI_GENERATED_CONTENT, FlagSeverity.HIGH,
                        "1 de 1 respuestas parecen redactadas por un asistente de IA", Instant.now())),
                "mock-1", Instant.now());
        assessment.recordFeedback("Tu puntaje ponderado fue 8 de 10.");
        when(queryService.handle(any(GetAssessmentByInterviewSessionIdQuery.class))).thenReturn(Optional.of(assessment));

        when(iam.fetchUserIdByUsername("rosa")).thenReturn(30L);
        when(iam.fetchUserIdByUsername("jorge")).thenReturn(31L);
        when(iam.fetchUserIdByUsername("recruiter.a")).thenReturn(10L);
        when(iam.fetchUserIdByUsername("recruiter.b")).thenReturn(20L);
        when(profiles.fetchCandidateIdByUserId(30L)).thenReturn(7L);
        when(profiles.fetchCandidateIdByUserId(31L)).thenReturn(8L);
        when(profiles.fetchCompanyIdByUserId(10L)).thenReturn(1L);
        when(profiles.fetchCompanyIdByUserId(20L)).thenReturn(2L);
        when(interviews.fetchCandidateIdBySessionId(SESSION_ID)).thenReturn(7L);
        when(interviews.fetchJobPostingIdBySessionId(SESSION_ID)).thenReturn(10L);
        when(recruitment.fetchCompanyIdByJobPostingId(10L)).thenReturn(1L);
    }

    @Test
    @WithMockUser(username = "rosa", roles = "CANDIDATE")
    void theOwningCandidateGetsTheirScoreBreakdownAndFeedbackWithoutRankingOrFlags() {
        var response = controller.getAssessment(SESSION_ID, SecurityContextHolder.getContext().getAuthentication());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(CandidateAssessmentResource.class);
        var json = objectMapper.writeValueAsString(response.getBody());
        assertThat(json).contains("\"weightedScore\":8.0", "Pensamiento analítico", "Tu puntaje ponderado fue 8 de 10.")
                .doesNotContain("integrityFlags", "AI_GENERATED_CONTENT", "rank", "candidateId", "confidence",
                        "engineVersion", "applicationId");
    }

    @Test
    @WithMockUser(username = "jorge", roles = "CANDIDATE")
    void anotherCandidateIsForbidden() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        assertThatThrownBy(() -> controller.getAssessment(SESSION_ID, auth)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(queryService);
    }

    @Test
    @WithMockUser(username = "recruiter.a", roles = "RECRUITER")
    void theOwningRecruiterStillSeesEverything() {
        var response = controller.getAssessment(SESSION_ID, SecurityContextHolder.getContext().getAuthentication());

        assertThat(response.getBody()).isInstanceOf(AssessmentResource.class);
        var body = (AssessmentResource) response.getBody();
        assertThat(body.integrityFlags()).singleElement()
                .satisfies(flag -> assertThat(flag.flagType()).isEqualTo(IntegrityFlagType.AI_GENERATED_CONTENT));
        assertThat(body.criterionScores()).singleElement().satisfies(score -> assertThat(score.confidence()).isEqualByComparingTo("0.80"));
        assertThat(body.feedbackSummary()).isEqualTo("Tu puntaje ponderado fue 8 de 10.");
    }

    @Test
    @WithMockUser(username = "recruiter.b", roles = "RECRUITER")
    void aRecruiterOfAnotherCompanyIsForbidden() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        assertThatThrownBy(() -> controller.getAssessment(SESSION_ID, auth)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void anAdminSeesEverything() {
        var response = controller.getAssessment(SESSION_ID, SecurityContextHolder.getContext().getAuthentication());

        assertThat(response.getBody()).isInstanceOf(AssessmentResource.class);
    }
}
