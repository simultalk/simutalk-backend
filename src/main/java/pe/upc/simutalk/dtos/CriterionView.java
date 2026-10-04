package pe.upc.simutalk.dtos;

/**
 * Read-only view of a job posting's evaluation criterion, shared through {@link RecruitmentContextFacade}.
 *
 * @param criterionType COMPETENCY or CERTIFICATION
 * @param weight        relative weight (1..100); the weights of a published posting add up to 100
 */
public record CriterionView(Long criterionId, String name, String description, int weight, String criterionType,
                            String certificationName, boolean mandatory) {

    public boolean isCompetency() {
        return "COMPETENCY".equals(criterionType);
    }
}
