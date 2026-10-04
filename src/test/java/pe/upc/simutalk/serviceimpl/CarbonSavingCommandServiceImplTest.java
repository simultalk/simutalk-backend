package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.config.SustainabilityProperties;
import pe.upc.simutalk.dtos.RecordCarbonSavingCommand;
import pe.upc.simutalk.repositories.CarbonSavingRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CarbonSavingCommandServiceImplTest {

    private final CarbonSavingRepository repository = mock(CarbonSavingRepository.class);
    private final AnalyticsExternalContextsService contexts = mock(AnalyticsExternalContextsService.class);
    private final SustainabilityProperties properties = new SustainabilityProperties(new BigDecimal("0.12"), 1.3, 12,
            List.of(new SustainabilityProperties.District("San Isidro", -12.0970, -77.0365),
                    new SustainabilityProperties.District("Comas", -11.9400, -77.0600)));
    private final CarbonSavingCommandServiceImpl service = new CarbonSavingCommandServiceImpl(repository, contexts, properties);

    @BeforeEach
    void setUp() {
        when(contexts.fetchCompanyIdByJobPostingId(10L)).thenReturn(1L);
        when(contexts.fetchCompanyDistrict(1L)).thenReturn("San Isidro");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void computesFromTheDistanceBetweenDistricts() {
        when(contexts.fetchCandidateDistrict(7L)).thenReturn("comas");

        var saving = service.handle(new RecordCarbonSavingCommand(50L, 10L, 7L)).orElseThrow();

        assertThat(saving.getDistanceKm().doubleValue()).isBetween(40.0, 50.0);
        assertThat(saving.getKgCo2eAvoided()).isEqualByComparingTo(
                saving.getDistanceKm().multiply(new BigDecimal("0.12")).setScale(3, java.math.RoundingMode.HALF_UP));
        assertThat(saving.getCompanyId()).isEqualTo(1L);
    }

    @Test
    void usesTheDefaultDistanceForUnknownDistricts() {
        when(contexts.fetchCandidateDistrict(7L)).thenReturn("Pucusana");

        var saving = service.handle(new RecordCarbonSavingCommand(50L, 10L, 7L)).orElseThrow();

        assertThat(saving.getDistanceKm()).isEqualByComparingTo("24.00");
    }

    @Test
    void isIdempotentPerApplication() {
        when(repository.existsByApplicationId(50L)).thenReturn(true);

        assertThat(service.handle(new RecordCarbonSavingCommand(50L, 10L, 7L))).isEmpty();
        verify(repository, never()).save(any());
    }
}
