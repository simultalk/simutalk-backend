package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CreateJobPostingCommand;
import pe.upc.simutalk.dtos.GetJobPostingIdsByCompanyIdQuery;
import pe.upc.simutalk.services.ApplicationCommandService;
import pe.upc.simutalk.services.IamContextFacade;
import pe.upc.simutalk.services.JobPostingCommandService;
import pe.upc.simutalk.services.JobPostingQueryService;
import pe.upc.simutalk.services.ProfilesContextFacade;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecruitmentDemoDataSeederTest {

    private final IamContextFacade iam = mock(IamContextFacade.class);
    private final ProfilesContextFacade profiles = mock(ProfilesContextFacade.class);
    private final JobPostingCommandService jobPostingCommandService = mock(JobPostingCommandService.class);
    private final JobPostingQueryService jobPostingQueryService = mock(JobPostingQueryService.class);

    private final RecruitmentDemoDataSeeder seeder = new RecruitmentDemoDataSeeder(true, iam, profiles,
            jobPostingCommandService, jobPostingQueryService, mock(ApplicationCommandService.class),
            new TransactionTemplate(mock(PlatformTransactionManager.class)));

    @Test
    void aFailureDraftingTheJobPostingIsLoggedAndNeverPropagates() {
        when(iam.fetchUserIdByUsername(any())).thenReturn(1L);
        when(profiles.fetchCompanyIdByUserId(1L)).thenReturn(10L);
        when(jobPostingQueryService.handle(any(GetJobPostingIdsByCompanyIdQuery.class))).thenReturn(List.of());
        when(jobPostingCommandService.handle(any(CreateJobPostingCommand.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatCode(() -> seeder.draftJobPosting(null)).doesNotThrowAnyException();
    }

    @Test
    void aFailurePublishingIsLoggedAndNeverPropagates() {
        when(iam.fetchUserIdByUsername(any())).thenThrow(new IllegalStateException("iam down"));

        assertThatCode(() -> seeder.publishAndApply(null)).doesNotThrowAnyException();
    }
}
