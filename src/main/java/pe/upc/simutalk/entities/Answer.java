package pe.upc.simutalk.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * What the candidate answered to a question. Belongs to the InterviewSession aggregate.
 * Only the answer is recorded here; scoring happens in assessment.
 * <p>
 * Rules: the transcript is not blank; a follow-up answer always points to the answer that
 * originated it ({@code parentAnswerId}).
 */
@Getter
@Entity
@Table(name = "answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer extends AuditableModel {

    public static final int AUDIO_URL_MAX_LENGTH = 500;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** The answer that originated this follow-up; only set when {@link #followUp} is true. */
    @Column(name = "parent_answer_id")
    private Long parentAnswerId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "audio_url", length = AUDIO_URL_MAX_LENGTH)
    private String audioUrl;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    @Column(name = "is_follow_up", nullable = false)
    private boolean followUp;

    public Answer(Long questionId, String transcript, String audioUrl, int durationSeconds, Instant answeredAt,
                  boolean followUp, Long parentAnswerId) {
        if (questionId == null || questionId <= 0) {
            throw new IllegalArgumentException("Question id must be a positive number");
        }
        if (transcript == null || transcript.isBlank()) {
            throw new IllegalArgumentException("Answer transcript is required");
        }
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("Answer duration must be positive");
        }
        if (answeredAt == null) {
            throw new IllegalArgumentException("Answer instant is required");
        }
        if (followUp && parentAnswerId == null) {
            throw new IllegalArgumentException("A follow-up answer requires the parent answer id");
        }
        if (!followUp && parentAnswerId != null) {
            throw new IllegalArgumentException("Only a follow-up answer can have a parent answer");
        }
        if (audioUrl != null && audioUrl.strip().length() > AUDIO_URL_MAX_LENGTH) {
            throw new IllegalArgumentException("Audio URL must be at most %d characters".formatted(AUDIO_URL_MAX_LENGTH));
        }
        this.questionId = questionId;
        this.transcript = transcript.strip();
        this.audioUrl = audioUrl == null || audioUrl.isBlank() ? null : audioUrl.strip();
        this.durationSeconds = durationSeconds;
        this.answeredAt = answeredAt;
        this.followUp = followUp;
        this.parentAnswerId = parentAnswerId;
    }
}
