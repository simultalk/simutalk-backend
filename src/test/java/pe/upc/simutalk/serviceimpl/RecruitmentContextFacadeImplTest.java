package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.ChangeApplicationStatusCommand;
import pe.upc.simutalk.dtos.CreateJobPostingCommand;
import pe.upc.simutalk.dtos.GetApplicationByIdQuery;
import pe.upc.simutalk.dtos.GetJobPostingByIdQuery;
import pe.upc.simutalk.entities.Application;
import pe.upc.simutalk.entities.JobPosting;
import pe.upc.simutalk.entities.Weight;
import pe.upc.simutalk.enums.ApplicationStatus;
import pe.upc.simutalk.enums.CriterionType;
import pe.upc.simutalk.services.ApplicationCommandService;
import pe.upc.simutalk.services.ApplicationQueryService;
import pe.upc.simutalk.services.JobPostingQueryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecruitmentContextFacadeImplTest {

    private final JobPostingQueryService postings = mock(JobPostingQueryService.class);
    private final ApplicationQueryService applications = mock(ApplicationQueryService.class);
    private final ApplicationCommandService applicationCommands = mock(ApplicationCommandService.class);
    private final RecruitmentContextFacadeImpl facade = new RecruitmentContextFacadeImpl(postings, applications, applicationCommands);

    @BeforeEach
    void setUp() {
        var posting = new JobPosting(new CreateJobPostingCommand("Analista", "SQL", 3L, null, false));
        ReflectionTestUtils.setField(posting, "id", 10L);
        var competency = posting.addCriterion("Análisis", "x", new Weight(70), CriterionType.COMPETENCY, null, false);
        ReflectionTestUtils.setField(competency, "id", 101L);
        var certification = posting.addCriterion("Cert", "x", new Weight(30), CriterionType.CERTIFICATION, "GDA", false);
        ReflectionTestUtils.setField(certification, "id", 102L);
        posting.publish(criterionId -> 1L);
        var application = Application.submit(posting, 7L, false, Instant.now());
        ReflectionTestUtils.setField(application, "id", 500L);

        when(postings.handle(any(GetJobPostingByIdQuery.class))).thenReturn(Optional.empty());
        when(postings.handle(argThat((GetJobPostingByIdQuery q) -> q != null && q.jobPostingId() == 10L)))
                .thenReturn(Optional.of(posting));
        when(applications.handle(any(GetApplicationByIdQuery.class))).thenReturn(Optional.empty());
        when(applications.handle(new GetApplicationByIdQuery(500L))).thenReturn(Optional.of(application));
    }

    @Test
    void answersAboutJobPostingsAndCriteria() {
        assertThat(facade.existsJobPostingById(10L)).isTrue();
        assertThat(facade.isJobPostingPublished(10L)).isTrue();
        assertThat(facade.isJobPostingDraft(10L)).isFalse();
        assertThat(facade.fetchCompanyIdByJobPostingId(10L)).isEqualTo(3L);
        assertThat(facade.fetchJobPostingTitle(10L)).isEqualTo("Analista");
        assertThat(facade.fetchJobPostingDescription(10L)).isEqualTo("SQL");
        assertThat(facade.existsCriterionInJobPosting(10L, 102L)).isTrue();
        assertThat(facade.fetchCompetencyCriterionIds(10L)).containsExactly(101L);
    }

    @Test
    void answersAboutApplications() {
        assertThat(facade.fetchJobPostingIdByApplicationId(500L)).isEqualTo(10L);
        assertThat(facade.fetchCandidateIdByApplicationId(500L)).isEqualTo(7L);
        assertThat(facade.fetchApplicationStatus(500L)).isEqualTo("RECEIVED");
    }

    @Test
    void neutralValuesForUnknownIds() {
        assertThat(facade.existsJobPostingById(99L)).isFalse();
        assertThat(facade.fetchCompanyIdByJobPostingId(99L)).isZero();
        assertThat(facade.fetchJobPostingTitle(99L)).isEmpty();
        assertThat(facade.fetchJobPostingDescription(null)).isEmpty();
        assertThat(facade.fetchCompetencyCriterionIds(99L)).isEmpty();
        assertThat(facade.existsCriterionInJobPosting(10L, null)).isFalse();
        assertThat(facade.fetchApplicationStatus(99L)).isEmpty();
        assertThat(facade.fetchCandidateIdByApplicationId(null)).isZero();
    }

    @Test
    void marksApplicationsThroughTheAggregateCommands() {
        facade.markApplicationAsInterviewing(500L);
        facade.markApplicationAsAssessed(500L);

        verify(applicationCommands).handle(new ChangeApplicationStatusCommand(500L, ApplicationStatus.INTERVIEWING));
        verify(applicationCommands).handle(new ChangeApplicationStatusCommand(500L, ApplicationStatus.ASSESSED));
    }
}
