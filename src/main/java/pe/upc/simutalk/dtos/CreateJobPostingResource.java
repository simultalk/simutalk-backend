package pe.upc.simutalk.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateJobPostingResource(
        @Schema(example = "Backend Developer Java Semi Senior")
        @NotBlank @Size(max = 150) String title,
        @Schema(example = "Desarrollo de APIs REST con Spring Boot para el área de pagos.")
        @NotBlank @Size(max = 4000) String description,
        @Schema(example = "2026-12-31")
        @FutureOrPresent LocalDate closingDate,
        @Schema(example = "true", description = "Si es true, los datos personales del postulante se ocultan durante la preselección")
        boolean anonymizedScreening) {
}
