package pe.upc.simutalk.repositories;

/** JPQL projection: average score of one criterion. */
public interface CriterionAverageRow {

    Long getCriterionId();

    String getCriterionName();

    Double getAverageScore();

    long getAssessedCount();
}
