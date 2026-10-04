package pe.upc.simutalk.entities;

import pe.upc.simutalk.dtos.CreateCandidateProfileCommand;
import pe.upc.simutalk.enums.VerificationStatus;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateProfileTest {

    private static final LocalDate ADULT_BIRTH_DATE = LocalDate.now().minusYears(25);
    private long nextId = 1;

    private CandidateProfile candidate(LocalDate birthDate, int yearsOfExperience) {
        return new CandidateProfile(new CreateCandidateProfileCommand(10L, "Rosa", "Quispe Mamani", "45879632",
                birthDate, "+51987654321", "San Juan de Lurigancho", yearsOfExperience));
    }

    private Certification add(CandidateProfile candidate, String code) {
        var certification = candidate.addCertification(
                new Certification("Cert " + nextId, "Coursera", code, LocalDate.now().minusMonths(6), null));
        ReflectionTestUtils.setField(certification, "id", nextId++);
        return certification;
    }

    @Test
    void createsAdultCandidate() {
        var candidate = candidate(ADULT_BIRTH_DATE, 3);

        assertThat(candidate.getPersonName().fullName()).isEqualTo("Rosa Quispe Mamani");
        assertThat(candidate.getDocumentNumber().documentType()).isEqualTo("DNI");
        assertThat(candidate.getAge(LocalDate.now())).isEqualTo(25);
    }

    @Test
    void rejectsCandidateUnder18() {
        var seventeen = LocalDate.now().minusYears(18).plusDays(1);

        assertThatThrownBy(() -> candidate(seventeen, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 18");
    }

    @Test
    void acceptsCandidateTurning18Today() {
        assertThat(candidate(LocalDate.now().minusYears(18), 0).getAge(LocalDate.now())).isEqualTo(18);
    }

    @Test
    void rejectsNegativeYearsOfExperience() {
        assertThatThrownBy(() -> candidate(ADULT_BIRTH_DATE, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
        var candidate = candidate(ADULT_BIRTH_DATE, 2);
        assertThatThrownBy(() -> candidate.updateDetails(new PersonName("Rosa", "Quispe"), ADULT_BIRTH_DATE,
                "+51987654321", "Comas", -3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidDocumentNumber() {
        assertThatThrownBy(() -> new CandidateProfile(new CreateCandidateProfileCommand(10L, "Rosa", "Quispe",
                "1234567", ADULT_BIRTH_DATE, "+51987654321", "Comas", 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void countsVerifiedCertifications() {
        var candidate = candidate(ADULT_BIRTH_DATE, 3);
        var first = add(candidate, "COURSERA-001");
        var second = add(candidate, "COURSERA-002");
        add(candidate, null);
        var rejected = add(candidate, "SHORT");

        candidate.verifyCertification(first.getId(), Instant.now());
        candidate.verifyCertification(second.getId(), Instant.now());
        candidate.rejectCertification(rejected.getId());

        assertThat(candidate.countVerifiedCertifications()).isEqualTo(2);
        assertThat(candidate.getCertifications()).hasSize(4);
    }

    @Test
    void countsRejectedCertifications() {
        var candidate = candidate(ADULT_BIRTH_DATE, 3);
        var rejected = add(candidate, "SHORT");
        var verified = add(candidate, "COURSERA-001");
        add(candidate, null);

        candidate.rejectCertification(rejected.getId());
        candidate.verifyCertification(verified.getId(), Instant.now());

        assertThat(candidate.countRejectedCertifications()).isEqualTo(1);
    }

    @Test
    void countCertificationsForScoringExcludesExpired() {
        var candidate = candidate(ADULT_BIRTH_DATE, 3);
        var expiring = candidate.addCertification(new Certification("PSM I", "Credly", "CREDLY-778899",
                LocalDate.now().minusYears(2), LocalDate.now().minusDays(1)));
        ReflectionTestUtils.setField(expiring, "id", 99L);
        candidate.verifyCertification(99L, Instant.now());

        assertThat(candidate.countVerifiedCertifications()).isEqualTo(1);
        assertThat(candidate.countCertificationsForScoring(LocalDate.now())).isZero();
    }

    @Test
    void certificationWithoutCodeCannotBeVerified() {
        var candidate = candidate(ADULT_BIRTH_DATE, 3);
        var withoutCode = add(candidate, null);

        assertThatThrownBy(() -> candidate.verifyCertification(withoutCode.getId(), Instant.now()))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(withoutCode.getVerificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
    }

    @Test
    void sameCredentialCannotBeRegisteredTwice() {
        var candidate = candidate(ADULT_BIRTH_DATE, 3);
        add(candidate, "COURSERA-001");

        assertThatThrownBy(() -> candidate.addCertification(new Certification("Otro", "coursera", " coursera-001 ",
                LocalDate.now().minusDays(3), null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void removesCertificationAndFailsForUnknownId() {
        var candidate = candidate(ADULT_BIRTH_DATE, 3);
        var certification = add(candidate, "COURSERA-001");

        candidate.removeCertification(certification.getId());

        assertThat(candidate.getCertifications()).isEmpty();
        assertThatThrownBy(() -> candidate.removeCertification(500L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void theEmailIsOptionalValidatedAndEditable() {
        var withoutEmail = candidate(ADULT_BIRTH_DATE, 3);
        assertThat(withoutEmail.getEmail()).isNull();

        var withEmail = new CandidateProfile(new CreateCandidateProfileCommand(10L, "Rosa", "Quispe Mamani", "45879632",
                ADULT_BIRTH_DATE, "+51987654321", "Comas", 3, "Rosa.Quispe@Example.com"));
        assertThat(withEmail.getEmail().value()).isEqualTo("rosa.quispe@example.com");

        withEmail.updateDetails(new PersonName("Rosa", "Quispe"), ADULT_BIRTH_DATE, "+51987654321", "Comas", 3);
        assertThat(withEmail.getEmail().value()).as("the update without e-mail keeps it").isEqualTo("rosa.quispe@example.com");
        withEmail.updateDetails(new PersonName("Rosa", "Quispe"), ADULT_BIRTH_DATE, "+51987654321", "Comas", 3, " ");
        assertThat(withEmail.getEmail()).as("a blank e-mail removes it").isNull();
        assertThatThrownBy(() -> withEmail.updateDetails(new PersonName("Rosa", "Quispe"), ADULT_BIRTH_DATE,
                "+51987654321", "Comas", 3, "rosa@")).isInstanceOf(IllegalArgumentException.class);
    }
}
