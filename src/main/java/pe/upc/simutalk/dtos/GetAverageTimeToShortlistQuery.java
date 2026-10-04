package pe.upc.simutalk.dtos;

import java.util.List;

public record GetAverageTimeToShortlistQuery(List<Long> jobPostingIds) {

    public GetAverageTimeToShortlistQuery {
        jobPostingIds = jobPostingIds == null ? List.of() : List.copyOf(jobPostingIds);
    }
}
