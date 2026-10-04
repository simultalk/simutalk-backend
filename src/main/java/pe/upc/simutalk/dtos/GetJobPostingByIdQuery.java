package pe.upc.simutalk.dtos;

/**
 * @param viewer who is asking; a DRAFT of another company is reported as not found
 */
public record GetJobPostingByIdQuery(Long jobPostingId, JobPostingViewer viewer) {

    public GetJobPostingByIdQuery {
        if (viewer == null) {
            throw new IllegalArgumentException("Viewer is required");
        }
    }
}
