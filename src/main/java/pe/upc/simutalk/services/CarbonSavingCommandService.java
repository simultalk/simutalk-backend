package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.RecordCarbonSavingCommand;
import pe.upc.simutalk.entities.CarbonSaving;

import java.util.Optional;

public interface CarbonSavingCommandService {

    /** @return the saving, or empty if the application already had one (idempotent) */
    Optional<CarbonSaving> handle(RecordCarbonSavingCommand command);
}
