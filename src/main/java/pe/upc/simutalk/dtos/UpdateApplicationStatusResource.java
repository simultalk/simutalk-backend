package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.ApplicationStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusResource(
        @Schema(example = "INTERVIEWING",
                description = "RECEIVED→INTERVIEWING→ASSESSED→SHORTLISTED→HIRED; REJECTED desde cualquier etapa no final")
        @NotNull ApplicationStatus status) {
}
