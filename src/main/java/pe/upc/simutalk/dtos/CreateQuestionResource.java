package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.QuestionOrigin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record CreateQuestionResource(
        @Schema(example = "1", description = "Criterio COMPETENCY de la misma vacante") @NotNull @Positive Long criterionId,
        @Schema(example = "Tienes una tabla de pedidos con clientes duplicados y fechas en formatos distintos. ¿Qué pasos seguirías para limpiarla?")
        @NotBlank @Size(max = 500) String statement,
        @Schema(example = "180", description = "Entre 30 y 600 segundos") @NotNull @Min(30) @Max(600) Integer maxDurationSeconds,
        @Schema(example = "MANUAL", description = "MANUAL por defecto") QuestionOrigin origin,
        @Schema(example = "false") boolean allowsFollowUp) {
}
