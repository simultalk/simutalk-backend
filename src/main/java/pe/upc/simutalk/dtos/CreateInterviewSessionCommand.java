package pe.upc.simutalk.dtos;

import java.time.LocalDate;

public record CreateInterviewSessionCommand(Long applicationId, LocalDate expiresAt) {
}
