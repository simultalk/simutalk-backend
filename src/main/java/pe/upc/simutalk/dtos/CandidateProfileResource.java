package pe.upc.simutalk.dtos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CandidateProfileResource(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String documentType,
        String documentNumber,
        String email,
        LocalDate birthDate,
        String phone,
        String district,
        int yearsOfExperience,
        long verifiedCertificationCount,
        List<CertificationResource> certifications,
        Instant createdAt,
        Instant updatedAt) {
}
