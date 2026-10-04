package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.ChangeApplicationStatusCommand;
import pe.upc.simutalk.dtos.CreateJobPostingCommand;
import pe.upc.simutalk.dtos.GetApplicationByIdQuery;
import pe.upc.simutalk.dtos.GetApplicationsByCandidateIdQuery;
import pe.upc.simutalk.dtos.GetApplicationsByJobPostingIdQuery;
import pe.upc.simutalk.dtos.GetJobPostingByIdQuery;
import pe.upc.simutalk.dtos.SubmitApplicationCommand;
import pe.upc.simutalk.dtos.UpdateApplicationStatusResource;
import pe.upc.simutalk.entities.Application;
import pe.upc.simutalk.entities.JobPosting;
import pe.upc.simutalk.entities.Weight;
import pe.upc.simutalk.enums.ApplicationStatus;
import pe.upc.simutalk.enums.CriterionType;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.securities.RecruitmentAccessPolicy;
import pe.upc.simutalk.services.ApplicationCommandService;
import pe.upc.simutalk.services.ApplicationQueryService;
import pe.upc.simutalk.services.IamContextFacade;
import pe.upc.simutalk.services.JobPostingQueryService;
import pe.upc.simutalk.services.ProfilesContextFacade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Real {@code @PreAuthorize} expressions of the application endpoints. Company 1 owns
 * job posting 100; application 500 belongs to it; candidate profile 7 belongs to user "candidate".
 */
@SpringJUnitWebConfig(ApplicationAuthorizationTest.Config.class)
class ApplicationAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean ApplicationCommandService applicationCommandService() { return mock(ApplicationCommandService.class); }
        @Bean ApplicationQueryService applicationQueryService() { return mock(ApplicationQueryService.class); }
        @Bean JobPostingQueryService jobPostingQueryService() { return mock(JobPostingQueryService.class); }
        @Bean IamContextFacade iamContextFacade() { return mock(IamContextFacade.class); }
        @Bean ProfilesContextFacade profilesContextFacade() { return mock(ProfilesContextFacade.class); }

        @Bean(name = "recruitmentAccess")
        RecruitmentAccessPolicy recruitmentAccess(IamContextFacade iam, ProfilesContextFacade profiles,
                                                  JobPostingQueryService postings, ApplicationQueryService applications) {
            return new RecruitmentAccessPolicy(iam, profiles, postings, applications);
        }

        @Bean JobPostingApplicationController jobPostingApplicationsController(
                ApplicationCommandService commands, ApplicationQueryService queries, RecruitmentAccessPolicy policy) {
            return new JobPostingApplicationController(commands, queries, policy);
        }

        @Bean ApplicationController applicationsController(
                ApplicationCommandService commands, ApplicationQueryService queries, RecruitmentAccessPolicy policy) {
            return new ApplicationController(commands, queries, policy);
        }
    }

    @Autowired JobPostingApplicationController postingApplications;
    @Autowired ApplicationController applications;
    @Autowired ApplicationCommandService commandService;
    @Autowired ApplicationQueryService applicationQueries;
    @Autowired JobPostingQueryService postingQueries;
    @Autowired IamContextFacade iam;
    @Autowired ProfilesContextFacade profiles;

    private Application application;

    @BeforeEach
    void setUp() {
        reset(commandService, applicationQueries, postingQueries, iam, profiles);
        var posting = new JobPosting(new CreateJobPostingCommand("Analista", "SQL", 1L, null, false));
        ReflectionTestUtils.setField(posting, "id", 100L);
        posting.addCriterion("Análisis", "x", new Weight(100), CriterionType.COMPETENCY, null, false);
        posting.publish(criterionId -> 1L);
        application = Application.submit(posting, 7L, false, Instant.now());
        ReflectionTestUtils.setField(application, "id", 500L);

        when(iam.fetchUserIdByUsername("recruiter.a")).thenReturn(10L);
        when(iam.fetchUserIdByUsername("recruiter.b")).thenReturn(20L);
        when(iam.fetchUserIdByUsername("candidate")).thenReturn(30L);
        when(iam.fetchUserIdByUsername("candidate.noprofile")).thenReturn(40L);
        when(profiles.fetchCompanyIdByUserId(10L)).thenReturn(1L);
        when(profiles.fetchCompanyIdByUserId(20L)).thenReturn(2L);
        when(profiles.fetchCandidateIdByUserId(30L)).thenReturn(7L);
        when(profiles.fetchCandidateIdByUserId(40L)).thenReturn(0L);

        when(postingQueries.handle(any(GetJobPostingByIdQuery.class))).thenReturn(Optional.of(posting));
        when(applicationQueries.handle(any(GetApplicationByIdQuery.class))).thenReturn(Optional.of(application));
        when(applicationQueries.handle(any(GetApplicationsByJobPostingIdQuery.class))).thenReturn(List.of(application));
        when(applicationQueries.handle(any(GetApplicationsByCandidateIdQuery.class))).thenReturn(List.of(application));
        when(commandService.handle(any(SubmitApplicationCommand.class))).thenReturn(application);
        when(commandService.handle(any(ChangeApplicationStatusCommand.class))).thenReturn(application);
    }

    private static org.springframework.security.core.Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static UpdateApplicationStatusResource interviewing() {
        return new UpdateApplicationStatusResource(ApplicationStatus.INTERVIEWING);
    }

    @Test
    @WithMockUser(username = "candidate", roles = "CANDIDATE")
    void candidateAppliesWithTheCandidateIdOfTheAuthenticatedUser() {
        var response = postingApplications.apply(100L, auth());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var command = ArgumentCaptor.forClass(SubmitApplicationCommand.class);
        verify(commandService).handle(command.capture());
        assertThat(command.getValue().candidateId()).isEqualTo(7L);
        assertThat(command.getValue().jobPostingId()).isEqualTo(100L);
    }

    @Test
    @WithMockUser(username = "candidate.noprofile", roles = "CANDIDATE")
    void candidateWithoutProfileCannotApply() {
        assertThatThrownBy(() -> postingApplications.apply(100L, auth()))
                .isInstanceOf(BusinessRuleViolationException.class);
        verifyNoInteractions(commandService);
    }

    @Test
    @WithMockUser(username = "candidate", roles = "CANDIDATE")
    void candidateCannotSeeThePipelineNorMoveApplications() {
        assertThatThrownBy(() -> postingApplications.getApplications(100L, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> applications.changeStatus(500L, interviewing())).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(commandService);
    }

    @Test
    @WithMockUser(username = "candidate", roles = "CANDIDATE")
    void candidateListsOwnApplications() {
        var response = applications.getMyApplications(auth());

        assertThat(response.getBody()).hasSize(1);
        verify(applicationQueries).handle(new GetApplicationsByCandidateIdQuery(7L));
    }

    @Test
    @WithMockUser(username = "recruiter.a", roles = "RECRUITER")
    void recruiterCannotApply() {
        assertThatThrownBy(() -> postingApplications.apply(100L, auth())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> applications.getMyApplications(auth())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "recruiter.b", roles = "RECRUITER")
    void recruiterOfAnotherCompanyIsForbidden() {
        assertThatThrownBy(() -> postingApplications.getApplications(100L, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> applications.changeStatus(500L, interviewing())).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(commandService);
    }

    @Test
    @WithMockUser(username = "recruiter.a", roles = "RECRUITER")
    void owningRecruiterSeesPipelineAndMovesApplications() {
        assertThat(postingApplications.getApplications(100L, ApplicationStatus.RECEIVED).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        verify(applicationQueries).handle(new GetApplicationsByJobPostingIdQuery(100L, ApplicationStatus.RECEIVED));
        assertThat(applications.changeStatus(500L, interviewing()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
