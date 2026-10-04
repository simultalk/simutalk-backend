package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.CertificationResource;
import pe.upc.simutalk.entities.Certification;

import java.time.LocalDate;

public class CertificationResourceFromEntityAssembler {

    public static CertificationResource toResourceFromEntity(Certification entity) {
        return new CertificationResource(entity.getId(), entity.getTitle(), entity.getIssuer(),
                entity.getCredentialCode(), entity.getIssuedAt(), entity.getExpiresAt(),
                entity.isExpired(LocalDate.now()), entity.getVerificationStatus(), entity.getVerifiedAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
