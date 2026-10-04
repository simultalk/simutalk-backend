package pe.upc.simutalk.dtos;

import java.time.LocalDate;

/**
 * Personal data of a candidate, shared through {@link ProfilesContextFacade}. Callers must
 * treat it as PII: it is used to redact texts before they leave the platform and to show the
 * candidate to authorized recruiters, and must never be sent to an external AI provider.
 */
public record CandidatePersonalData(Long candidateId, String firstName, String lastName, String documentNumber,
                                    String phone, String district, LocalDate birthDate, String email) {

    /** Data of a candidate without e-mail. */
    public CandidatePersonalData(Long candidateId, String firstName, String lastName, String documentNumber,
                                 String phone, String district, LocalDate birthDate) {
        this(candidateId, firstName, lastName, documentNumber, phone, district, birthDate, null);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }
}
