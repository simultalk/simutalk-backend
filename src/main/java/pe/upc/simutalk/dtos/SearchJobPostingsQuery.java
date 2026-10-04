package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.JobPostingStatus;

/**
 * Lists job postings. Both filters are optional; a {@code null} filter is ignored.
 * Results are limited to what the {@code viewer} may see in a listing.
 */
public record SearchJobPostingsQuery(Long companyId, JobPostingStatus status, JobPostingViewer viewer) {

    public SearchJobPostingsQuery {
        if (viewer == null) {
            throw new IllegalArgumentException("Viewer is required");
        }
    }
}
