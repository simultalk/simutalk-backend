package pe.upc.simutalk.dtos;

import java.time.LocalDate;

public record CreateCandidateProfileCommand(
        Long userId,
        String firstName,
        String lastName,
        String documentNumber,
        LocalDate birthDate,
        String phone,
        String district,
        int yearsOfExperience,
        String email) {

    /** A profile without e-mail. */
    public CreateCandidateProfileCommand(Long userId, String firstName, String lastName, String documentNumber,
                                         LocalDate birthDate, String phone, String district, int yearsOfExperience) {
        this(userId, firstName, lastName, documentNumber, birthDate, phone, district, yearsOfExperience, null);
    }
}
