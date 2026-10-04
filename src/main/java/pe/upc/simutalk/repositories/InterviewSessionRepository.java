package pe.upc.simutalk.repositories;

import pe.upc.simutalk.entities.InterviewSession;
import pe.upc.simutalk.enums.InterviewSessionStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Finders load the session together with its answers (open-in-view is disabled).
 */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    @EntityGraph(attributePaths = "answers")
    Optional<InterviewSession> findWithAnswersById(Long id);

    @EntityGraph(attributePaths = "answers")
    Optional<InterviewSession> findByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);

    boolean existsByJobPostingIdAndCandidateIdAndStatus(Long jobPostingId, Long candidateId, InterviewSessionStatus status);
}
