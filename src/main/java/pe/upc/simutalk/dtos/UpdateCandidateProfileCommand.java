package pe.upc.simutalk.dtos;

import java.time.LocalDate;

/** The document number is not part of the update: it cannot change. */
public record UpdateCandidateProfileCommand(
        Long candidateId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String phone,
        String district,
        int yearsOfExperience,
        String email) {

    /** Updates without e-mail, which leaves the profile without one. */
    public UpdateCandidateProfileCommand(Long candidateId, String firstName, String lastName, LocalDate birthDate,
                                         String phone, String district, int yearsOfExperience) {
        this(candidateId, firstName, lastName, birthDate, phone, district, yearsOfExperience, null);
    }
}
