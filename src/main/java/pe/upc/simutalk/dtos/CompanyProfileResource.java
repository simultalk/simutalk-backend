package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.CompanySize;

import java.time.Instant;

public record CompanyProfileResource(
        Long id,
        Long userId,
        String legalName,
        String tradeName,
        String industry,
        String ruc,
        CompanySize companySize,
        String district,
        String email,
        Instant createdAt,
        Instant updatedAt) {
}
