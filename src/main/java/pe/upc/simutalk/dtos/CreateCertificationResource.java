package pe.upc.simutalk.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCertificationResource(
        @Schema(example = "Google Data Analytics") @NotBlank @Size(max = 150) String title,
        @Schema(example = "Coursera") @NotBlank @Size(max = 100) String issuer,
        @Schema(example = "COURSERA-7XK29QPL", description = "Opcional. Sin código la certificación nunca podrá verificarse")
        @Size(max = 100) String credentialCode,
        @Schema(example = "2025-06-10") @NotNull @PastOrPresent LocalDate issuedAt,
        @Schema(example = "2028-06-10", description = "Opcional") LocalDate expiresAt) {
}
