package pe.upc.simutalk.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record RecordAnswerResource(
        @Schema(example = "1") @NotNull @Positive Long questionId,
        @Schema(example = "Primero perfilaría la tabla: contaría duplicados por documento y revisaría los formatos de fecha...")
        @NotBlank String transcript,
        @Schema(example = "https://storage.example.com/audio/abc.webm", description = "Opcional") @Size(max = 500) String audioUrl,
        @Schema(example = "95") @NotNull @Positive Integer durationSeconds,
        @Schema(example = "false", description = "true si responde a una repregunta") boolean followUp,
        @Schema(description = "Obligatorio si followUp es true: la respuesta que originó la repregunta") Long parentAnswerId) {
}
