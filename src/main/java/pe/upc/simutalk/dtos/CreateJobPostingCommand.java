package pe.upc.simutalk.dtos;

import java.time.LocalDate;

public record CreateJobPostingCommand(
        String title,
        String description,
        Long companyId,
        LocalDate closingDate,
        boolean anonymizedScreening) {
}
