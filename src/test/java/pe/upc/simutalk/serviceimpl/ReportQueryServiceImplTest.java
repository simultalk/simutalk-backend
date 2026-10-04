package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.GetCarbonSavingsReportQuery;
import pe.upc.simutalk.dtos.GetCompanySummaryReportQuery;
import pe.upc.simutalk.dtos.GetFunnelReportQuery;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.repositories.CarbonSavingRepository;
import pe.upc.simutalk.repositories.CarbonSavingTotals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportQueryServiceImplTest {

    private final AnalyticsExternalContextsService contexts = mock(AnalyticsExternalContextsService.class);
    private final CarbonSavingRepository savings = mock(CarbonSavingRepository.class);
    private final ReportQueryServiceImpl service = new ReportQueryServiceImpl(contexts, savings);

    @BeforeEach
    void setUp() {
        when(contexts.existsJobPosting(1L)).thenReturn(true);
    }

    @Test
    void funnelListsEveryStageInPipelineOrder() {
        when(contexts.countApplicationsByStatus(1L)).thenReturn(Map.of("ASSESSED", 2L, "RECEIVED", 3L, "INTERVIEWING", 1L));

        var report = service.handle(new GetFunnelReportQuery(1L));

        assertThat(report.stages().keySet()).containsExactly("RECEIVED", "INTERVIEWING", "ASSESSED", "SHORTLISTED", "HIRED", "REJECTED");
        assertThat(report.stages()).containsEntry("SHORTLISTED", 0L).containsEntry("RECEIVED", 3L);
        assertThat(report.totalApplications()).isEqualTo(6);
    }

    @Test
    void unknownJobPostingIsNotFound() {
        assertThatThrownBy(() -> service.handle(new GetFunnelReportQuery(99L))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void carbonReportUsesTheDatabaseTotals() {
        when(savings.totalsByJobPostingId(1L)).thenReturn(new CarbonSavingTotals() {
            public BigDecimal getTotalKg() { return new BigDecimal("4.56789"); }
            public BigDecimal getTotalKm() { return new BigDecimal("38.1"); }
            public long getTrips() { return 2; }
        });

        var report = service.handle(new GetCarbonSavingsReportQuery(1L));

        assertThat(report.totalKgCo2eAvoided()).isEqualByComparingTo("4.568");
        assertThat(report.tripsAvoided()).isEqualTo(2);
        assertThat(report.totalKmAvoided()).isEqualByComparingTo("38.10");
    }

    @Test
    void companySummaryConvertsSecondsToDays() {
        when(contexts.fetchJobPostingIds(5L)).thenReturn(List.of(1L, 2L));
        when(contexts.countPublishedJobPostings(5L)).thenReturn(1L);
        when(contexts.countAssessments(List.of(1L, 2L))).thenReturn(3L);
        when(contexts.countEvidences(List.of(1L, 2L))).thenReturn(19L);
        when(contexts.fetchAverageSecondsToShortlist(List.of(1L, 2L))).thenReturn(302_400L);

        var report = service.handle(new GetCompanySummaryReportQuery(5L));

        assertThat(report.activeJobPostings()).isEqualTo(1);
        assertThat(report.assessedCandidates()).isEqualTo(3);
        assertThat(report.anchoredEvidences()).isEqualTo(19);
        assertThat(report.averageDaysToShortlist()).isEqualByComparingTo("3.5");
    }

    @Test
    void averageDaysIsNullWhenNobodyWasShortlisted() {
        when(contexts.fetchJobPostingIds(5L)).thenReturn(List.of());
        when(contexts.fetchAverageSecondsToShortlist(List.of())).thenReturn(null);

        assertThat(service.handle(new GetCompanySummaryReportQuery(5L)).averageDaysToShortlist()).isNull();
    }
}
