package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.repositories.CandidateProfileRepository;
import pe.upc.simutalk.repositories.CompanyProfileRepository;
import pe.upc.simutalk.services.CandidateProfileCommandService;
import pe.upc.simutalk.services.CompanyProfileCommandService;
import pe.upc.simutalk.services.IamContextFacade;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProfileDemoDataSeederTest {

    private final IamContextFacade iam = mock(IamContextFacade.class);
    private final CompanyProfileRepository companyProfileRepository = mock(CompanyProfileRepository.class);
    private final CandidateProfileRepository candidateProfileRepository = mock(CandidateProfileRepository.class);

    private final ProfileDemoDataSeeder seeder = new ProfileDemoDataSeeder(true, "Demo12345", iam,
            mock(CompanyProfileCommandService.class), mock(CandidateProfileCommandService.class),
            companyProfileRepository, candidateProfileRepository,
            new TransactionTemplate(mock(PlatformTransactionManager.class)));

    @Test
    void skipsWhenProfilesAlreadyExist() {
        when(companyProfileRepository.count()).thenReturn(1L);

        seeder.on(null);

        verifyNoInteractions(iam);
    }

    @Test
    void aFailureIsLoggedAndNeverPropagates() {
        when(iam.signUpUserIfAbsent(anyString(), anyString(), any())).thenThrow(new IllegalStateException("iam down"));

        assertThatCode(() -> seeder.on(null)).doesNotThrowAnyException();
    }
}
