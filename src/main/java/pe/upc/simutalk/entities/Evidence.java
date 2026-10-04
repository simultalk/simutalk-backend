package pe.upc.simutalk.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Text fragment of an interview answer that supports a criterion score. Offsets are
 * zero-based, end-exclusive positions in the ORIGINAL transcript (not the anonymized one).
 */
@Getter
@Entity
@Table(name = "evidences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Evidence extends AuditableModel {

    @Column(name = "answer_id", nullable = false)
    private Long answerId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "start_offset", nullable = false)
    private int startOffset;

    @Column(name = "end_offset", nullable = false)
    private int endOffset;

    public Evidence(Long answerId, String excerpt, int startOffset, int endOffset) {
        if (answerId == null || answerId <= 0) {
            throw new IllegalArgumentException("Answer id must be a positive number");
        }
        if (excerpt == null || excerpt.isBlank()) {
            throw new IllegalArgumentException("Evidence excerpt is required");
        }
        if (startOffset < 0 || startOffset >= endOffset) {
            throw new IllegalArgumentException("Evidence offsets must satisfy 0 <= start < end (got %d, %d)"
                    .formatted(startOffset, endOffset));
        }
        this.answerId = answerId;
        this.excerpt = excerpt;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }
}
