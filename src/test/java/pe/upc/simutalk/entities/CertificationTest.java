package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.VerificationStatus;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertificationTest {

    private static final LocalDate ISSUED = LocalDate.now().minusYears(1);

    @Test
    void isBornUnverified() {
        var certification = new Certification("Google Data Analytics", "Coursera", "ABCD1234", ISSUED, null);

        assertThat(certification.getVerificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
        assertThat(certification.getVerifiedAt()).isNull();
    }

    @Test
    void markVerifiedWithoutCredentialCodeThrows() {
        var withoutCode = new Certification("Scrum Foundation", "CertiProf", null, ISSUED, null);
        var blankCode = new Certification("Scrum Foundation", "CertiProf", "   ", ISSUED, null);

        assertThatThrownBy(() -> withoutCode.markVerified(Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("without credential code");
        assertThatThrownBy(() -> blankCode.markVerified(Instant.now()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(withoutCode.getVerificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
    }

    @Test
    void markVerifiedWithCredentialCodeMovesToVerified() {
        var certification = new Certification("AWS Cloud Practitioner", "Credly", "AWS-998877", ISSUED, null);
        var now = Instant.now();

        certification.markVerified(now);

        assertThat(certification.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(certification.getVerifiedAt()).isEqualTo(now);
    }

    @Test
    void cannotVerifyTwiceOrVerifyARejectedCertification() {
        var verified = new Certification("AWS", "Credly", "AWS-998877", ISSUED, null);
        verified.markVerified(Instant.now());
        var rejected = new Certification("AWS", "Credly", "AWS-111", ISSUED, null);
        rejected.markRejected();

        assertThatThrownBy(() -> verified.markVerified(Instant.now())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> rejected.markVerified(Instant.now())).isInstanceOf(IllegalStateException.class);
        assertThat(rejected.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    void isExpiredOnlyWhenExpirationDateHasPassed() {
        var today = LocalDate.of(2026, 9, 27);
        var noExpiration = new Certification("Java", "Coursera", "X", LocalDate.of(2024, 1, 1), null);
        var expiresToday = new Certification("Java", "Coursera", "X", LocalDate.of(2024, 1, 1), today);
        var expired = new Certification("Java", "Coursera", "X", LocalDate.of(2024, 1, 1), today.minusDays(1));

        assertThat(noExpiration.isExpired(today)).isFalse();
        assertThat(expiresToday.isExpired(today)).isFalse();
        assertThat(expired.isExpired(today)).isTrue();
    }

    @Test
    void countsForScoringOnlyWhenVerifiedAndNotExpired() {
        var today = LocalDate.of(2026, 9, 27);
        var unverified = new Certification("Java", "Coursera", "ABCD1234", LocalDate.of(2024, 1, 1), null);
        var verified = new Certification("Java", "Coursera", "ABCD1234", LocalDate.of(2024, 1, 1), null);
        verified.markVerified(Instant.now());
        var verifiedExpired = new Certification("Java", "Coursera", "ABCD1234", LocalDate.of(2024, 1, 1), today.minusDays(1));
        verifiedExpired.markVerified(Instant.now());

        assertThat(unverified.countsForScoring(today)).isFalse();
        assertThat(verified.countsForScoring(today)).isTrue();
        assertThat(verifiedExpired.countsForScoring(today)).isFalse();
    }

    @Test
    void rejectsInvalidDates() {
        assertThatThrownBy(() -> new Certification("Java", "Coursera", null, LocalDate.now().plusDays(1), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Certification("Java", "Coursera", null, ISSUED, ISSUED.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Certification("Java", "Coursera", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
