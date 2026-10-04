package pe.upc.simutalk.entities;

import pe.upc.simutalk.dtos.CreateJobPostingCommand;
import pe.upc.simutalk.enums.ApplicationStatus;
import pe.upc.simutalk.enums.CriterionType;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.upc.simutalk.enums.ApplicationStatus.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTest {

    private JobPosting jobPosting;

    @BeforeEach
    void setUp() {
        jobPosting = new JobPosting(new CreateJobPostingCommand("Analista de Datos", "SQL y Python", 1L, null, true));
        ReflectionTestUtils.setField(jobPosting, "id", 50L);
        jobPosting.addCriterion("Pensamiento analítico", "x", new Weight(100), CriterionType.COMPETENCY, null, false);
    }

    private Application submitted() {
        jobPosting.publish(criterionId -> 1L);
        return Application.submit(jobPosting, 7L, false, Instant.now());
    }

    private Application in(ApplicationStatus status) {
        var application = submitted();
        ReflectionTestUtils.setField(application, "status", status);
        return application;
    }

    @Test
    void submittedApplicationStartsReceivedAndKeepsOnlyIds() {
        var appliedAt = Instant.now();
        jobPosting.publish(criterionId -> 1L);

        var application = Application.submit(jobPosting, 7L, false, appliedAt);

        assertThat(application.getStatus()).isEqualTo(RECEIVED);
        assertThat(application.getJobPostingId()).isEqualTo(50L);
        assertThat(application.getCandidateId()).isEqualTo(7L);
        assertThat(application.getAppliedAt()).isEqualTo(appliedAt);
    }

    @Test
    void cannotApplyToDraftPosting() {
        assertThatThrownBy(() -> Application.submit(jobPosting, 7L, false, Instant.now()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("PUBLISHED");
    }

    @Test
    void cannotApplyToClosedPosting() {
        jobPosting.publish(criterionId -> 1L);
        jobPosting.close();

        assertThatThrownBy(() -> Application.submit(jobPosting, 7L, false, Instant.now()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void candidateCannotApplyTwiceToTheSamePosting() {
        jobPosting.publish(criterionId -> 1L);

        assertThatThrownBy(() -> Application.submit(jobPosting, 7L, true, Instant.now()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already applied");
    }

    @ParameterizedTest
    @CsvSource({
            "RECEIVED, INTERVIEWING", "RECEIVED, REJECTED",
            "INTERVIEWING, ASSESSED", "INTERVIEWING, REJECTED",
            "ASSESSED, SHORTLISTED", "ASSESSED, REJECTED",
            "SHORTLISTED, HIRED", "SHORTLISTED, REJECTED"
    })
    void allowsDirectedTransitions(ApplicationStatus from, ApplicationStatus to) {
        var application = in(from);

        application.changeStatus(to);

        assertThat(application.getStatus()).isEqualTo(to);
    }

    @ParameterizedTest
    @CsvSource({
            "RECEIVED, ASSESSED", "RECEIVED, SHORTLISTED", "RECEIVED, HIRED", "RECEIVED, RECEIVED",
            "INTERVIEWING, RECEIVED", "INTERVIEWING, SHORTLISTED", "INTERVIEWING, HIRED",
            "ASSESSED, RECEIVED", "ASSESSED, INTERVIEWING", "ASSESSED, HIRED",
            "SHORTLISTED, RECEIVED", "SHORTLISTED, ASSESSED",
            "REJECTED, RECEIVED", "REJECTED, INTERVIEWING", "REJECTED, HIRED",
            "HIRED, REJECTED", "HIRED, SHORTLISTED", "HIRED, RECEIVED"
    })
    void rejectsInvalidJumpsWithIllegalStateException(ApplicationStatus from, ApplicationStatus to) {
        var application = in(from);

        assertThatThrownBy(() -> application.changeStatus(to))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(from + " to " + to);
        assertThat(application.getStatus()).isEqualTo(from);
    }

    @Test
    void recordsWhenTheApplicationWasShortlisted() {
        var application = in(ASSESSED);
        var shortlistedAt = Instant.parse("2026-10-01T15:00:00Z");

        application.changeStatus(SHORTLISTED, shortlistedAt);

        assertThat(application.getShortlistedAt()).isEqualTo(shortlistedAt);
        application.changeStatus(HIRED, shortlistedAt.plusSeconds(3600));
        assertThat(application.getShortlistedAt()).isEqualTo(shortlistedAt);
    }

    @Test
    void walksTheWholePipelineToHired() {
        var application = submitted();

        application.changeStatus(INTERVIEWING);
        application.changeStatus(ASSESSED);
        application.changeStatus(SHORTLISTED);
        application.changeStatus(HIRED);

        assertThat(application.getStatus()).isEqualTo(HIRED);
        assertThat(HIRED.isFinal()).isTrue();
        assertThat(REJECTED.isFinal()).isTrue();
    }
}
