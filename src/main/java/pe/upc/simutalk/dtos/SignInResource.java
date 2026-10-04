package pe.upc.simutalk.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SignInResource(
        @Schema(example = "ana.torres") @NotBlank String username,
        @Schema(example = "S3cure-Passw0rd") @NotBlank String password) {
}
