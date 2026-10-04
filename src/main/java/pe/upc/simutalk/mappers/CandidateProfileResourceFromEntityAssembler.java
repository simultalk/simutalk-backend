package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.CandidateProfileResource;
import pe.upc.simutalk.entities.CandidateProfile;

import java.time.LocalDate;

public class CandidateProfileResourceFromEntityAssembler {

    public static CandidateProfileResource toResourceFromEntity(CandidateProfile entity) {
        var certifications = entity.getCertifications().stream()
                .map(CertificationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return new CandidateProfileResource(entity.getId(), entity.getUserId(),
                entity.getPersonName().firstName(), entity.getPersonName().lastName(),
                entity.getDocumentNumber().documentType(), entity.getDocumentNumber().value(),
                entity.getEmail() == null ? null : entity.getEmail().value(), entity.getBirthDate(), entity.getPhone(), entity.getDistrict(), entity.getYearsOfExperience(),
                entity.countCertificationsForScoring(LocalDate.now()), certifications,
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
