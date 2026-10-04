package pe.upc.simutalk.repositories;

import pe.upc.simutalk.entities.Application;
import pe.upc.simutalk.enums.ApplicationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJobPostingIdAndCandidateId(Long jobPostingId, Long candidateId);

    List<Application> findAllByJobPostingIdOrderByAppliedAtAsc(Long jobPostingId);

    List<Application> findAllByJobPostingIdAndStatusOrderByAppliedAtAsc(Long jobPostingId, ApplicationStatus status);

    List<Application> findAllByCandidateIdOrderByAppliedAtDesc(Long candidateId);

    /** Applications per pipeline stage of a job posting (aggregated in the database). */
    @Query("select a.status as status, count(a) as total from Application a "
            + "where a.jobPostingId = :jobPostingId group by a.status")
    List<ApplicationStatusCount> countByStatusForJobPosting(@Param("jobPostingId") Long jobPostingId);

    /** Average seconds from applying to SHORTLISTED, or null when nobody was shortlisted. */
    @Query("select avg(extract(epoch from a.shortlistedAt) - extract(epoch from a.appliedAt)) from Application a "
            + "where a.jobPostingId in :jobPostingIds and a.shortlistedAt is not null")
    Double averageSecondsToShortlist(@Param("jobPostingIds") List<Long> jobPostingIds);
}
