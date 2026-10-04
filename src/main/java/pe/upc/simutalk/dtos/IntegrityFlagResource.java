package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.FlagSeverity;
import pe.upc.simutalk.enums.IntegrityFlagType;

import java.time.Instant;

public record IntegrityFlagResource(Long id, IntegrityFlagType flagType, FlagSeverity severity, String detail,
                                    Instant raisedAt) {
}
