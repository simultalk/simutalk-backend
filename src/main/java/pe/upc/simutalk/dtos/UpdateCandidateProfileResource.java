package pe.upc.simutalk.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** The document number cannot be changed. */
public record UpdateCandidateProfileResource(
        @Schema(example = "Rosa") @NotBlank @Size(max = 80) String firstName,
        @Schema(example = "Quispe Mamani") @NotBlank @Size(max = 80) String lastName,
        @Schema(example = "1998-04-15") @NotNull @Past LocalDate birthDate,
        @Schema(example = "+51987654321") @NotBlank @Size(max = 20) String phone,
        @Schema(example = "Comas") @NotBlank @Size(max = 80) String district,
        @Schema(example = "4") @NotNull @PositiveOrZero Integer yearsOfExperience,
        @Schema(example = "rosa.quispe@example.com", description = "Opcional. Correo de contacto para notificaciones; vacío lo elimina") @Email @Size(max = 254) String email) {
}
