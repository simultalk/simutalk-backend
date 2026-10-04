package pe.upc.simutalk.services;

/**
 * Port the JobPosting aggregate uses on publish to check that every COMPETENCY criterion has
 * interview questions. Implemented through the interviews context's ACL.
 */
@FunctionalInterface
public interface InterviewQuestionCounter {

    long countQuestionsByCriterionId(Long criterionId);
}
