package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.ApplicationStatus;

/**
 * An application moved to a new stage. Published by recruitment after the change and handled after the
 * transaction commits, so a listener never sees (or notifies) a change that was rolled back.
 */
public record ApplicationStatusChangedEvent(Long applicationId, Long jobPostingId, Long candidateId,
                                            ApplicationStatus status) {
}
