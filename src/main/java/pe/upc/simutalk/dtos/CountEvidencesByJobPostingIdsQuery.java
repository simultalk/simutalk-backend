package pe.upc.simutalk.dtos;

import java.util.List;

public record CountEvidencesByJobPostingIdsQuery(List<Long> jobPostingIds) {

    public CountEvidencesByJobPostingIdsQuery {
        jobPostingIds = jobPostingIds == null ? List.of() : List.copyOf(jobPostingIds);
    }
}
