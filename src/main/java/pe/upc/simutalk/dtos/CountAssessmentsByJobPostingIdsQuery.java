package pe.upc.simutalk.dtos;

import java.util.List;

public record CountAssessmentsByJobPostingIdsQuery(List<Long> jobPostingIds) {

    public CountAssessmentsByJobPostingIdsQuery {
        jobPostingIds = jobPostingIds == null ? List.of() : List.copyOf(jobPostingIds);
    }
}
