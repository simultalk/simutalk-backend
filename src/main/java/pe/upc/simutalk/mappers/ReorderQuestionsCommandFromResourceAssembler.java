package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.ReorderQuestionsCommand;
import pe.upc.simutalk.dtos.ReorderQuestionsResource;

public class ReorderQuestionsCommandFromResourceAssembler {

    public static ReorderQuestionsCommand toCommandFromResource(Long jobPostingId, ReorderQuestionsResource resource) {
        return new ReorderQuestionsCommand(jobPostingId, resource.orderedIds());
    }
}
