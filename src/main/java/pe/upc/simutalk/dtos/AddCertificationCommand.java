package pe.upc.simutalk.dtos;

import java.time.LocalDate;

public record AddCertificationCommand(
        Long candidateId,
        String title,
        String issuer,
        String credentialCode,
        LocalDate issuedAt,
        LocalDate expiresAt) {
}
