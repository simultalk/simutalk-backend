package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.CreateQuestionCommand;
import pe.upc.simutalk.dtos.CreateQuestionResource;
import pe.upc.simutalk.enums.QuestionOrigin;

public class CreateQuestionCommandFromResourceAssembler {

    public static CreateQuestionCommand toCommandFromResource(Long jobPostingId, CreateQuestionResource resource) {
        return new CreateQuestionCommand(jobPostingId, resource.criterionId(), resource.statement(),
                resource.maxDurationSeconds(), resource.origin() == null ? QuestionOrigin.MANUAL : resource.origin(),
                resource.allowsFollowUp());
    }
}
