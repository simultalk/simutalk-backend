package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.CriterionOrigin;
import pe.upc.simutalk.enums.CriterionType;

public record AddEvaluationCriterionCommand(
        Long jobPostingId,
        String name,
        String description,
        Integer weight,
        CriterionType criterionType,
        String certificationName,
        boolean mandatory,
        CriterionOrigin origin) {

    /** A manually defined criterion. */
    public AddEvaluationCriterionCommand(Long jobPostingId, String name, String description, Integer weight,
                                         CriterionType criterionType, String certificationName, boolean mandatory) {
        this(jobPostingId, name, description, weight, criterionType, certificationName, mandatory, CriterionOrigin.MANUAL);
    }
}
