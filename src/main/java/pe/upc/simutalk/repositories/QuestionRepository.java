package pe.upc.simutalk.repositories;

import pe.upc.simutalk.entities.Question;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByJobPostingIdOrderByPositionAscIdAsc(Long jobPostingId);

    long countByJobPostingId(Long jobPostingId);

    long countByCriterionId(Long criterionId);
}
