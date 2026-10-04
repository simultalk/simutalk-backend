package pe.upc.simutalk.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateInterviewSessionResource(
        @Schema(example = "2026-10-15", description = "Último día en que el candidato puede iniciar la entrevista")
        @NotNull @FutureOrPresent LocalDate expiresAt) {
}
