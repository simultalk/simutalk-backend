package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.CriterionType;

public record UpdateEvaluationCriterionCommand(
        Long jobPostingId,
        Long criterionId,
        String name,
        String description,
        Integer weight,
        CriterionType criterionType,
        String certificationName,
        boolean mandatory) {
}
