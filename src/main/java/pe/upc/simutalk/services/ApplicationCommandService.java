package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.ChangeApplicationStatusCommand;
import pe.upc.simutalk.dtos.SubmitApplicationCommand;
import pe.upc.simutalk.entities.Application;

public interface ApplicationCommandService {

    Application handle(SubmitApplicationCommand command);

    Application handle(ChangeApplicationStatusCommand command);
}
