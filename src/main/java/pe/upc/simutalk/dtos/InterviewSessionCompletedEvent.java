package pe.upc.simutalk.dtos;

import java.time.Instant;

/**
 * Integration event published by interviews when a candidate completes an asynchronous
 * interview. Listeners should react after the publisher's transaction commits.
 */
public record InterviewSessionCompletedEvent(Long interviewSessionId, Long applicationId, Long jobPostingId,
                                             Long candidateId, Instant completedAt) {
}
