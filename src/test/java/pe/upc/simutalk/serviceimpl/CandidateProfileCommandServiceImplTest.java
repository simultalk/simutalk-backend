package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.services.CredentialVerificationService.VerificationResult;

import pe.upc.simutalk.dtos.CreateCandidateProfileCommand;
import pe.upc.simutalk.dtos.VerifyCertificationCommand;
import pe.upc.simutalk.entities.CandidateProfile;
import pe.upc.simutalk.entities.Certification;
import pe.upc.simutalk.enums.VerificationStatus;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.repositories.CandidateProfileRepository;
import pe.upc.simutalk.repositories.CompanyProfileRepository;
import pe.upc.simutalk.services.CredentialVerificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CandidateProfileCommandServiceImplTest {

    private final CandidateProfileRepository candidates = mock(CandidateProfileRepository.class);
    private final CompanyProfileRepository companies = mock(CompanyProfileRepository.class);
    private final ExternalIamService iam = mock(ExternalIamService.class);
    private final CredentialVerificationService verifier = mock(CredentialVerificationService.class);
    private final CandidateProfileCommandServiceImpl service =
            new CandidateProfileCommandServiceImpl(candidates, companies, iam, verifier);

    private CandidateProfile candidate;
    private Certification certification;

    private static CreateCandidateProfileCommand createCommand() {
        return new CreateCandidateProfileCommand(10L, "Rosa", "Quispe", "45879632",
                LocalDate.now().minusYears(25), "+51987654321", "Comas", 2);
    }

    @BeforeEach
    void setUp() {
        candidate = new CandidateProfile(createCommand());
        certification = candidate.addCertification(new Certification("PSM I", "Credly", "CREDLY-778899",
                LocalDate.now().minusMonths(3), null));
        ReflectionTestUtils.setField(certification, "id", 1L);
        when(candidates.findWithCertificationsById(5L)).thenReturn(Optional.of(candidate));
        when(candidates.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createFailsWhenUserDoesNotExist() {
        when(iam.existsUser(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.handle(createCommand()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("does not exist");
        verify(candidates, never()).save(any());
    }

    @Test
    void createFailsWhenUserAlreadyHasAnyProfile() {
        when(iam.existsUser(10L)).thenReturn(true);
        when(companies.existsByUserId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.handle(createCommand()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already has a profile");
    }

    @Test
    void matchedVerificationMarksVerified() {
        when(verifier.verify(any(), any(), any(), any())).thenReturn(new VerificationResult(true, "ok"));

        var outcome = service.handle(new VerifyCertificationCommand(5L, 1L));

        assertThat(outcome.certification().getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(outcome.certification().getVerifiedAt()).isNotNull();
        verify(verifier).verify("Credly", "CREDLY-778899", "PSM I", "Rosa Quispe");
    }

    @Test
    void unmatchedVerificationMarksRejected() {
        when(verifier.verify(any(), any(), any(), any())).thenReturn(new VerificationResult(false, "no match"));

        var outcome = service.handle(new VerifyCertificationCommand(5L, 1L));

        assertThat(outcome.certification().getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    void unavailableIssuerLeavesUnverified() {
        when(verifier.verify(any(), any(), any(), any())).thenReturn(VerificationResult.unavailable("timeout"));

        var outcome = service.handle(new VerifyCertificationCommand(5L, 1L));

        assertThat(outcome.certification().getVerificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
        assertThat(outcome.result().conclusive()).isFalse();
    }

    @Test
    void certificationWithoutCodeIsNeverSentToTheIssuer() {
        var withoutCode = candidate.addCertification(new Certification("Scrum", "CertiProf", null,
                LocalDate.now().minusMonths(1), null));
        ReflectionTestUtils.setField(withoutCode, "id", 2L);

        assertThatThrownBy(() -> service.handle(new VerifyCertificationCommand(5L, 2L)))
                .isInstanceOf(BusinessRuleViolationException.class);
        verifyNoInteractions(verifier);
    }
}
