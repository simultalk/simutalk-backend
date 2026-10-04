package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.repositories.AssessmentRepository;
import pe.upc.simutalk.services.AnswerScoringService;
import pe.upc.simutalk.services.AssessmentCommandService;
import pe.upc.simutalk.services.IamContextFacade;
import pe.upc.simutalk.services.InterviewsContextFacade;
import pe.upc.simutalk.services.ProfilesContextFacade;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssessmentDemoDataSeederTest {

    @Test
    void aFailureIsLoggedAndNeverPropagates() {
        var iam = mock(IamContextFacade.class);
        when(iam.fetchUserIdByUsername(any())).thenThrow(new IllegalStateException("iam down"));
        var seeder = new AssessmentDemoDataSeeder(true, mock(AssessmentCommandService.class),
                mock(AnswerScoringService.class), mock(AssessmentRepository.class), iam,
                mock(ProfilesContextFacade.class), mock(RecruitmentContextFacade.class),
                mock(InterviewsContextFacade.class));

        assertThatCode(() -> seeder.on(null)).doesNotThrowAnyException();
    }
}
