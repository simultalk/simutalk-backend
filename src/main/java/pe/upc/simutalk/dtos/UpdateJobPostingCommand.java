package pe.upc.simutalk.dtos;

import java.time.LocalDate;

public record UpdateJobPostingCommand(
        Long jobPostingId,
        String title,
        String description,
        LocalDate closingDate,
        boolean anonymizedScreening) {
}
