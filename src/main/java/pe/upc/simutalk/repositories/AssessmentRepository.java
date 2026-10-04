package pe.upc.simutalk.repositories;

import pe.upc.simutalk.entities.Assessment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Finders fetch the criterion scores; evidences and integrity flags are loaded in batches
 * ({@code @BatchSize}) inside the query service's transaction.
 */
@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    @EntityGraph(attributePaths = "criterionScores")
    Optional<Assessment> findWithScoresById(Long id);

    @EntityGraph(attributePaths = "criterionScores")
    Optional<Assessment> findWithScoresByInterviewSessionId(Long interviewSessionId);

    @EntityGraph(attributePaths = "criterionScores")
    List<Assessment> findAllWithScoresByJobPostingId(Long jobPostingId);

    boolean existsByInterviewSessionId(Long interviewSessionId);

    @Query("select cs.criterionId as criterionId, cs.criterionName as criterionName, avg(cs.score) as averageScore, "
            + "count(cs) as assessedCount from Assessment a join a.criterionScores cs "
            + "where a.jobPostingId = :jobPostingId group by cs.criterionId, cs.criterionName order by cs.criterionId")
    List<CriterionAverageRow> averageScoreByCriterion(@Param("jobPostingId") Long jobPostingId);

    long countByJobPostingIdIn(List<Long> jobPostingIds);

    @Query("select count(e) from Assessment a join a.criterionScores cs join cs.evidences e where a.jobPostingId in :jobPostingIds")
    long countEvidencesByJobPostingIds(@Param("jobPostingIds") List<Long> jobPostingIds);
}
