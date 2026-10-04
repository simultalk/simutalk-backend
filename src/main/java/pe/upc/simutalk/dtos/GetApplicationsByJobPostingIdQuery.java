package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.ApplicationStatus;

/**
 * @param status optional pipeline stage filter; {@code null} returns every stage
 */
public record GetApplicationsByJobPostingIdQuery(Long jobPostingId, ApplicationStatus status) {
}
