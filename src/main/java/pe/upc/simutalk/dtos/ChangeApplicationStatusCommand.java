package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.ApplicationStatus;

public record ChangeApplicationStatusCommand(Long applicationId, ApplicationStatus status) {
}
