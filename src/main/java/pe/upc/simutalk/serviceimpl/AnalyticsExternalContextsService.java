package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CriterionAverageView;
import pe.upc.simutalk.services.AssessmentContextFacade;
import pe.upc.simutalk.services.ProfilesContextFacade;
import pe.upc.simutalk.services.RecruitmentContextFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Anti-corruption layer from analytics to recruitment, assessment and profiles. Every figure
 * comes from an aggregation done by the owning context; analytics never reads their tables.
 */
@Service("analyticsExternalContextsService")
@RequiredArgsConstructor
public class AnalyticsExternalContextsService {

    private final RecruitmentContextFacade recruitmentContextFacade;
    private final AssessmentContextFacade assessmentContextFacade;
    private final ProfilesContextFacade profilesContextFacade;

    public boolean existsJobPosting(Long jobPostingId) {
        return recruitmentContextFacade.existsJobPostingById(jobPostingId);
    }

    public Long fetchCompanyIdByJobPostingId(Long jobPostingId) {
        return recruitmentContextFacade.fetchCompanyIdByJobPostingId(jobPostingId);
    }

    public Map<String, Long> countApplicationsByStatus(Long jobPostingId) {
        return recruitmentContextFacade.countApplicationsByStatus(jobPostingId);
    }

    public long countPublishedJobPostings(Long companyId) {
        return recruitmentContextFacade.countPublishedJobPostingsByCompanyId(companyId);
    }

    public List<Long> fetchJobPostingIds(Long companyId) {
        return recruitmentContextFacade.fetchJobPostingIdsByCompanyId(companyId);
    }

    public Long fetchAverageSecondsToShortlist(List<Long> jobPostingIds) {
        return recruitmentContextFacade.fetchAverageSecondsToShortlist(jobPostingIds);
    }

    public List<CriterionAverageView> fetchCriterionAverages(Long jobPostingId) {
        return assessmentContextFacade.fetchCriterionAverages(jobPostingId);
    }

    public long countAssessments(List<Long> jobPostingIds) {
        return assessmentContextFacade.countAssessmentsByJobPostingIds(jobPostingIds);
    }

    public long countEvidences(List<Long> jobPostingIds) {
        return assessmentContextFacade.countEvidencesByJobPostingIds(jobPostingIds);
    }

    public String fetchCandidateDistrict(Long candidateId) {
        return profilesContextFacade.fetchCandidateDistrict(candidateId);
    }

    public String fetchCompanyDistrict(Long companyId) {
        return profilesContextFacade.fetchCompanyDistrict(companyId);
    }
}
