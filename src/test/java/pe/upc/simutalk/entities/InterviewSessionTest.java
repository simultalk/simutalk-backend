package pe.upc.simutalk.entities;

import pe.upc.simutalk.enums.InterviewSessionStatus;
import pe.upc.simutalk.enums.QuestionOrigin;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.upc.simutalk.enums.InterviewSessionStatus.*;

class InterviewSessionTest {

    private static final long JOB_POSTING_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

    private InterviewSession session;
    private Question sqlQuestion;
    private Question cleaningQuestion;
    private Question communicationQuestion;
    private long nextAnswerId = 100;

    private static Question question(long id, boolean allowsFollowUp) {
        var question = new Question(JOB_POSTING_ID, 10L, "Pregunta " + id, 120, (int) id, QuestionOrigin.MANUAL, allowsFollowUp);
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    @BeforeEach
    void setUp() {
        session = new InterviewSession(50L, JOB_POSTING_ID, 7L, TODAY.plusDays(7), Instant.now());
        sqlQuestion = question(1, true);
        cleaningQuestion = question(2, false);
        communicationQuestion = question(3, false);
    }

    private Answer answer(Question question, int seconds) {
        return persisted(new Answer(question.getId(), "Respuesta a " + question.getId(), null, seconds, Instant.now(), false, null));
    }

    private Answer followUp(Question question, Long parentAnswerId) {
        return persisted(new Answer(question.getId(), "Repregunta de " + question.getId(), null, 40, Instant.now(), true, parentAnswerId));
    }

    private Answer persisted(Answer answer) {
        ReflectionTestUtils.setField(answer, "id", nextAnswerId++);
        return answer;
    }

    private void startSession() {
        session.start(TODAY, Instant.now());
    }

    @Test
    void startsFromPendingAndSealsStartedAt() {
        startSession();

        assertThat(session.getStatus()).isEqualTo(IN_PROGRESS);
        assertThat(session.getStartedAt()).isNotNull();
    }

    @Test
    void startFromInvalidStateFails() {
        startSession();

        assertThatThrownBy(this::startSession)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void cannotStartOncePastDue() {
        assertThatThrownBy(() -> session.start(TODAY.plusDays(8), Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
        assertThat(session.getStatus()).isEqualTo(PENDING);
    }

    @Test
    void canStartOnTheExpirationDay() {
        session.start(TODAY.plusDays(7), Instant.now());

        assertThat(session.getStatus()).isEqualTo(IN_PROGRESS);
    }

    @Test
    void answeringWhilePendingFails() {
        assertThatThrownBy(() -> session.recordAnswer(answer(sqlQuestion, 60), sqlQuestion))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IN_PROGRESS");
        assertThat(session.getAnswers()).isEmpty();
    }

    @Test
    void answeringTheSameQuestionTwiceFails() {
        startSession();
        session.recordAnswer(answer(cleaningQuestion, 60), cleaningQuestion);

        assertThatThrownBy(() -> session.recordAnswer(answer(cleaningQuestion, 30), cleaningQuestion))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already been answered");
        assertThat(session.getAnswers()).hasSize(1);
    }

    @Test
    void followUpWithoutExistingParentFails() {
        startSession();

        assertThatThrownBy(() -> session.recordAnswer(followUp(sqlQuestion, 999L), sqlQuestion))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("parent answer");
    }

    @Test
    void followUpOnQuestionThatDoesNotAllowItFails() {
        startSession();
        var parent = session.recordAnswer(answer(cleaningQuestion, 60), cleaningQuestion);

        assertThatThrownBy(() -> session.recordAnswer(followUp(cleaningQuestion, parent.getId()), cleaningQuestion))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("does not allow follow-up");
    }

    @Test
    void secondFollowUpToTheSameQuestionFails() {
        startSession();
        var parent = session.recordAnswer(answer(sqlQuestion, 60), sqlQuestion);
        session.recordAnswer(followUp(sqlQuestion, parent.getId()), sqlQuestion);

        assertThatThrownBy(() -> session.recordAnswer(followUp(sqlQuestion, parent.getId()), sqlQuestion))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already has a follow-up");
        assertThat(session.getAnswers()).hasSize(2);
    }

    @Test
    void followUpMustReplyToTheRegularAnswerOfTheSameQuestion() {
        startSession();
        var otherQuestionAnswer = session.recordAnswer(answer(cleaningQuestion, 60), cleaningQuestion);

        assertThatThrownBy(() -> session.recordAnswer(followUp(sqlQuestion, otherQuestionAnswer.getId()), sqlQuestion))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void rejectsAnswersToQuestionsOfAnotherScript() {
        startSession();
        var foreign = new Question(2L, 10L, "Otra vacante", 120, 1, QuestionOrigin.MANUAL, false);
        ReflectionTestUtils.setField(foreign, "id", 77L);

        assertThatThrownBy(() -> session.recordAnswer(answer(foreign, 60), foreign))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void completeWithMissingAnswersFails() {
        startSession();
        session.recordAnswer(answer(sqlQuestion, 60), sqlQuestion);
        session.recordAnswer(answer(cleaningQuestion, 90), cleaningQuestion);

        assertThatThrownBy(() -> session.complete(3, Instant.now()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("1 of 3");
        assertThat(session.getStatus()).isEqualTo(IN_PROGRESS);
    }

    @Test
    void followUpDoesNotCountAsAnsweringAnotherQuestion() {
        startSession();
        var parent = session.recordAnswer(answer(sqlQuestion, 60), sqlQuestion);
        session.recordAnswer(followUp(sqlQuestion, parent.getId()), sqlQuestion);
        session.recordAnswer(answer(cleaningQuestion, 90), cleaningQuestion);

        assertThat(session.answeredQuestionCount()).isEqualTo(2);
        assertThatThrownBy(() -> session.complete(3, Instant.now())).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void completesWhenEveryQuestionIsAnswered() {
        startSession();
        var parent = session.recordAnswer(answer(sqlQuestion, 60), sqlQuestion);
        session.recordAnswer(followUp(sqlQuestion, parent.getId()), sqlQuestion);
        session.recordAnswer(answer(cleaningQuestion, 90), cleaningQuestion);
        session.recordAnswer(answer(communicationQuestion, 120), communicationQuestion);
        var finishedAt = Instant.now();

        session.complete(3, finishedAt);

        assertThat(session.getStatus()).isEqualTo(COMPLETED);
        assertThat(session.getFinishedAt()).isEqualTo(finishedAt);
        assertThat(session.getTotalDurationSeconds()).isEqualTo(60 + 40 + 90 + 120);
    }

    @Test
    void completeFromPendingFails() {
        assertThatThrownBy(() -> session.complete(0, Instant.now())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void completedSessionIsFinal() {
        startSession();
        session.recordAnswer(answer(sqlQuestion, 60), sqlQuestion);
        session.complete(1, Instant.now());

        assertThatThrownBy(() -> session.recordAnswer(answer(cleaningQuestion, 60), cleaningQuestion))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> session.expire(TODAY.plusDays(30))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(this::startSession).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void expiresFromPendingOncePastDue() {
        session.expire(TODAY.plusDays(8));

        assertThat(session.getStatus()).isEqualTo(EXPIRED);
        assertThatThrownBy(this::startSession).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void expiresFromInProgressOncePastDue() {
        startSession();
        session.recordAnswer(answer(sqlQuestion, 60), sqlQuestion);

        session.expire(TODAY.plusDays(8));

        assertThat(session.getStatus()).isEqualTo(EXPIRED);
        assertThatThrownBy(() -> session.recordAnswer(answer(cleaningQuestion, 60), cleaningQuestion))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotExpireBeforeTheDueDate() {
        assertThatThrownBy(() -> session.expire(TODAY.plusDays(7)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valid until");
        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.PENDING);
    }
}
