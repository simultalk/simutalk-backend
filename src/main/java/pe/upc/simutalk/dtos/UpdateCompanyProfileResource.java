package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.CompanySize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The RUC cannot be changed. */
public record UpdateCompanyProfileResource(
        @Schema(example = "Consultora Andina S.A.C.") @NotBlank @Size(max = 150) String legalName,
        @Schema(example = "Andina TI") @NotBlank @Size(max = 150) String tradeName,
        @Schema(example = "Consultoría de TI") @NotBlank @Size(max = 150) String industry,
        @Schema(example = "GRANDE") @NotNull CompanySize companySize,
        @Schema(example = "Miraflores") @NotBlank @Size(max = 80) String district,
        @Schema(example = "seleccion@consultoraandina.example", description = "Opcional. Correo de contacto; vacío lo elimina") @Email @Size(max = 254) String email) {
}
