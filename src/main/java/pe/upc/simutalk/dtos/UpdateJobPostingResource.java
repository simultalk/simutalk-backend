package pe.upc.simutalk.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateJobPostingResource(
        @Schema(example = "Backend Developer Java Senior")
        @NotBlank @Size(max = 150) String title,
        @Schema(example = "Desarrollo y mantenimiento de APIs REST con Spring Boot.")
        @NotBlank @Size(max = 4000) String description,
        @Schema(example = "2027-01-31")
        @FutureOrPresent LocalDate closingDate,
        @Schema(example = "true")
        boolean anonymizedScreening) {
}
