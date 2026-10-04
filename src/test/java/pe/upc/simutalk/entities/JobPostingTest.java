package pe.upc.simutalk.entities;

import pe.upc.simutalk.dtos.CreateJobPostingCommand;
import pe.upc.simutalk.dtos.JobPostingViewer;
import pe.upc.simutalk.enums.CriterionOrigin;
import pe.upc.simutalk.enums.CriterionType;
import pe.upc.simutalk.enums.JobPostingStatus;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.services.InterviewQuestionCounter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobPostingTest {

    /** Every criterion has interview questions. */
    private static final InterviewQuestionCounter ALL_COVERED = criterionId -> 1L;

    private JobPosting jobPosting;

    @BeforeEach
    void setUp() {
        jobPosting = new JobPosting(new CreateJobPostingCommand(
                "Backend Developer", "APIs REST con Spring Boot", 1L, LocalDate.now().plusDays(30), true));
    }

    @Test
    void startsAsDraftWithoutCriteria() {
        assertThat(jobPosting.getStatus()).isEqualTo(JobPostingStatus.DRAFT);
        assertThat(jobPosting.getCriteria()).isEmpty();
        assertThat(jobPosting.getTotalWeight()).isZero();
    }

    @Test
    void publishFailsWithoutCriteria() {
        assertThatThrownBy(() -> jobPosting.publish(ALL_COVERED))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("at least one evaluation criterion");
        assertThat(jobPosting.getStatus()).isEqualTo(JobPostingStatus.DRAFT);
    }

    @Test
    void publishFailsWhenWeightsAddUpToLessThanOneHundred() {
        addCompetency("Comunicación", 60);
        addCompetency("Trabajo en equipo", 30);

        assertThatThrownBy(() -> jobPosting.publish(ALL_COVERED))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("current total: 90");
    }

    @Test
    void publishFailsWhenWeightsAddUpToMoreThanOneHundred() {
        addCompetency("Comunicación", 60);
        addCompetency("Trabajo en equipo", 50);

        assertThatThrownBy(() -> jobPosting.publish(ALL_COVERED))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("current total: 110");
    }

    @Test
    void publishSucceedsWhenWeightsAddUpToExactlyOneHundred() {
        addCompetency("Comunicación", 60);
        jobPosting.addCriterion("Java SE", "Certificación vigente", new Weight(40),
                CriterionType.CERTIFICATION, "Oracle Certified Professional Java SE 21", true);

        jobPosting.publish(ALL_COVERED);

        assertThat(jobPosting.getStatus()).isEqualTo(JobPostingStatus.PUBLISHED);
        assertThat(jobPosting.getTotalWeight()).isEqualTo(100);
    }

    @Test
    void criteriaCannotChangeOnceTheJobPostingIsPublished() {
        var criterion = addCompetency("Comunicación", 100);
        jobPosting.publish(ALL_COVERED);

        assertThatThrownBy(() -> addCompetency("Liderazgo", 10))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> jobPosting.removeCriterion(criterion.getId()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void criterionNamesAreUniqueIgnoringCase() {
        addCompetency("Comunicación", 50);

        assertThatThrownBy(() -> addCompetency("  comunicación ", 50))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void certificationCriterionRequiresCertificationName() {
        assertThatThrownBy(() -> jobPosting.addCriterion("AWS", "Certificación cloud", new Weight(20),
                CriterionType.CERTIFICATION, " ", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void competencyCriterionDiscardsCertificationData() {
        var criterion = jobPosting.addCriterion("Comunicación", "Claridad", new Weight(30),
                CriterionType.COMPETENCY, "Algo", true);

        assertThat(criterion.getCertificationName()).isNull();
        assertThat(criterion.isMandatory()).isFalse();
    }

    @Test
    void updateAndRemoveCriterionById() {
        var criterion = addCompetency("Comunicación", 30);

        jobPosting.updateCriterion(criterion.getId(), "Comunicación oral", "Claridad al hablar", new Weight(45),
                CriterionType.COMPETENCY, null, false);
        assertThat(jobPosting.getTotalWeight()).isEqualTo(45);
        assertThat(criterion.getName()).isEqualTo("Comunicación oral");

        jobPosting.removeCriterion(criterion.getId());
        assertThat(jobPosting.getCriteria()).isEmpty();
    }

    @Test
    void updatingUnknownCriterionFails() {
        assertThatThrownBy(() -> jobPosting.removeCriterion(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cannotGoBackToDraft() {
        assertThatThrownBy(() -> jobPosting.changeStatus(JobPostingStatus.DRAFT, ALL_COVERED))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void closedJobPostingIsReadOnly() {
        jobPosting.changeStatus(JobPostingStatus.CLOSED, ALL_COVERED);

        assertThatThrownBy(() -> jobPosting.updateDetails("Otro", "Otra", null, true))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(jobPosting::close).isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> jobPosting.publish(ALL_COVERED)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void anonymizedScreeningIsFixedAfterPublishing() {
        addCompetency("Comunicación", 100);
        jobPosting.publish(ALL_COVERED);

        assertThatThrownBy(() -> jobPosting.updateDetails("Backend Developer", "APIs", null, false))
                .isInstanceOf(BusinessRuleViolationException.class);

        jobPosting.updateDetails("Backend Developer Sr", "APIs", null, true);
        assertThat(jobPosting.getTitle()).isEqualTo("Backend Developer Sr");
    }

    @Test
    void publishedJobPostingCannotBeDeleted() {
        addCompetency("Comunicación", 100);
        jobPosting.publish(ALL_COVERED);

        assertThatThrownBy(jobPosting::ensureCanBeDeleted).isInstanceOf(BusinessRuleViolationException.class);

        jobPosting.close();
        jobPosting.ensureCanBeDeleted();
    }

    @Test
    void draftIsVisibleOnlyToItsOwnCompany() {
        assertThat(jobPosting.isOwnedBy(1L)).isTrue();
        assertThat(jobPosting.isOwnedBy(2L)).isFalse();
        assertThat(jobPosting.isVisibleTo(JobPostingViewer.ofCompany(1L))).isTrue();
        assertThat(jobPosting.isVisibleTo(JobPostingViewer.ofCompany(2L))).isFalse();
        assertThat(jobPosting.isVisibleTo(JobPostingViewer.ofCompany(null))).isFalse();
        assertThat(jobPosting.isVisibleTo(JobPostingViewer.unrestrictedViewer())).isTrue();
    }

    @Test
    void listingsShowOtherCompaniesOnlyPublishedPostings() {
        var otherCompany = JobPostingViewer.ofCompany(2L);
        assertThat(jobPosting.isListedFor(otherCompany)).isFalse();

        addCompetency("Comunicación", 100);
        jobPosting.publish(ALL_COVERED);
        assertThat(jobPosting.isListedFor(otherCompany)).isTrue();
        assertThat(jobPosting.isVisibleTo(otherCompany)).isTrue();

        jobPosting.close();
        assertThat(jobPosting.isListedFor(otherCompany)).isFalse();
        assertThat(jobPosting.isVisibleTo(otherCompany)).isTrue();
        assertThat(jobPosting.isListedFor(JobPostingViewer.ofCompany(1L))).isTrue();
    }

    @Test
    void publishFailsWhenACompetencyCriterionHasNoInterviewQuestions() {
        var communication = addCompetency("Comunicación", 40);
        addCompetency("Trabajo en equipo", 30);
        jobPosting.addCriterion("AWS", "Certificación", new Weight(30), CriterionType.CERTIFICATION,
                "AWS Cloud Practitioner", false);
        InterviewQuestionCounter onlyCommunicationCovered =
                criterionId -> communication.getId().equals(criterionId) ? 2L : 0L;

        assertThatThrownBy(() -> jobPosting.publish(onlyCommunicationCovered))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Trabajo en equipo")
                .hasMessageNotContaining("AWS");
        assertThat(jobPosting.getStatus()).isEqualTo(JobPostingStatus.DRAFT);
    }

    @Test
    void certificationCriteriaDoNotNeedInterviewQuestions() {
        var communication = addCompetency("Comunicación", 70);
        jobPosting.addCriterion("AWS", "Certificación", new Weight(30), CriterionType.CERTIFICATION,
                "AWS Cloud Practitioner", false);

        jobPosting.publish(criterionId -> communication.getId().equals(criterionId) ? 1L : 0L);

        assertThat(jobPosting.getStatus()).isEqualTo(JobPostingStatus.PUBLISHED);
    }

    private long nextId = 1;

    private EvaluationCriterion addCompetency(String name, int weight) {
        var criterion = jobPosting.addCriterion(name, "Descripción de " + name, new Weight(weight),
                CriterionType.COMPETENCY, null, false);
        ReflectionTestUtils.setField(criterion, "id", nextId++);
        return criterion;
    }

    @Test
    void criteriaAreManualByDefault() {
        var criterion = jobPosting.addCriterion("Liderazgo", "Guía al equipo", new Weight(100),
                CriterionType.COMPETENCY, null, false);

        assertThat(criterion.getOrigin()).isEqualTo(CriterionOrigin.MANUAL);
    }

    @Test
    void criteriaCanOnlyBeSuggestedWhileTheJobPostingIsDraft() {
        jobPosting.ensureCriteriaCanBeSuggested();
        jobPosting.addCriterion("Liderazgo", "Guía al equipo", new Weight(100), CriterionType.COMPETENCY, null, false);
        jobPosting.publish(ALL_COVERED);

        assertThatThrownBy(jobPosting::ensureCriteriaCanBeSuggested)
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DRAFT");
    }
}
