package pe.upc.simutalk.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateCandidateProfileResource(
        @Schema(example = "2") @NotNull @Positive Long userId,
        @Schema(example = "Rosa") @NotBlank @Size(max = 80) String firstName,
        @Schema(example = "Quispe Mamani") @NotBlank @Size(max = 80) String lastName,
        @Schema(example = "45879632", description = "DNI (8 dígitos) o CE (9 a 12 dígitos)") @NotBlank String documentNumber,
        @Schema(example = "1998-04-15", description = "El candidato debe tener al menos 18 años") @NotNull @Past LocalDate birthDate,
        @Schema(example = "+51987654321") @NotBlank @Size(max = 20) String phone,
        @Schema(example = "San Juan de Lurigancho") @NotBlank @Size(max = 80) String district,
        @Schema(example = "3") @NotNull @PositiveOrZero Integer yearsOfExperience,
        @Schema(example = "rosa.quispe@example.com", description = "Opcional. Correo de contacto para notificaciones; nunca se envía al proveedor de IA") @Email @Size(max = 254) String email) {
}
