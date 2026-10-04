package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.ComputeAssessmentCommand;
import pe.upc.simutalk.entities.Assessment;

public interface AssessmentCommandService {

    Assessment handle(ComputeAssessmentCommand command);
}
