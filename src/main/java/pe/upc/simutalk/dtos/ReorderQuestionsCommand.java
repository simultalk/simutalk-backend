package pe.upc.simutalk.dtos;

import java.util.List;

/**
 * @param orderedIds every question id of the job posting's script, in the new order
 */
public record ReorderQuestionsCommand(Long jobPostingId, List<Long> orderedIds) {

    public ReorderQuestionsCommand {
        orderedIds = orderedIds == null ? List.of() : List.copyOf(orderedIds);
    }
}
