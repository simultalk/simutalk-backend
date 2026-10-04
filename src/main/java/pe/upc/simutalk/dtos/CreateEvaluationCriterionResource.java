package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.CriterionOrigin;
import pe.upc.simutalk.enums.CriterionType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record CreateEvaluationCriterionResource(
        @Schema(example = "Comunicación efectiva")
        @NotBlank @Size(max = 100) String name,
        @Schema(example = "Explica ideas técnicas con claridad y estructura.")
        @NotBlank @Size(max = 1000) String description,
        @Schema(example = "40", description = "Peso relativo, entero entre 1 y 100")
        @NotNull @Min(1) @Max(100) Integer weight,
        @Schema(example = "COMPETENCY")
        @NotNull CriterionType criterionType,
        @Schema(description = "Obligatorio si criterionType es CERTIFICATION; se ignora en COMPETENCY", example = "Oracle Certified Professional Java SE 21")
        @Size(max = 150) String certificationName,
        @Schema(description = "Solo aplica a CERTIFICATION; se ignora en COMPETENCY", example = "false")
        boolean mandatory,
        @Schema(description = "MANUAL por defecto; AI_SUGGESTED cuando el reclutador acepta una sugerencia. "
                + "El peso lo pone siempre el reclutador.", example = "MANUAL")
        CriterionOrigin origin) {
}
