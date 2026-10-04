package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.JobPostingStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateJobPostingStatusResource(
        @Schema(example = "PUBLISHED", description = "Estado destino: PUBLISHED o CLOSED")
        @NotNull JobPostingStatus status) {
}
