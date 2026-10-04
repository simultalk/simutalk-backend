package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.JobPostingStatus;

public record ChangeJobPostingStatusCommand(Long jobPostingId, JobPostingStatus status) {
}
