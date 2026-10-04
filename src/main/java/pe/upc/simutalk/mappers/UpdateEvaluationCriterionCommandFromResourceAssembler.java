package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.UpdateEvaluationCriterionCommand;
import pe.upc.simutalk.dtos.UpdateEvaluationCriterionResource;

public class UpdateEvaluationCriterionCommandFromResourceAssembler {

    public static UpdateEvaluationCriterionCommand toCommandFromResource(Long jobPostingId, Long criterionId,
                                                                         UpdateEvaluationCriterionResource resource) {
        return new UpdateEvaluationCriterionCommand(jobPostingId, criterionId, resource.name(), resource.description(),
                resource.weight(), resource.criterionType(), resource.certificationName(), resource.mandatory());
    }
}
