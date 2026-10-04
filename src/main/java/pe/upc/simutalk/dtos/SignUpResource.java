package pe.upc.simutalk.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SignUpResource(
        @Schema(example = "ana.torres")
        @NotBlank @Size(min = 3, max = 50) String username,
        @Schema(example = "S3cure-Passw0rd")
        @NotBlank @Size(min = 8, max = 72) String password,
        @Schema(description = "ROLE_CANDIDATE o ROLE_RECRUITER. Vacío = ROLE_CANDIDATE. ROLE_ADMIN no se acepta.",
                example = "[\"ROLE_RECRUITER\"]")
        List<String> roles) {
}
