package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.config.SustainabilityProperties;
import pe.upc.simutalk.dtos.GeoPoint;
import pe.upc.simutalk.dtos.RecordCarbonSavingCommand;
import pe.upc.simutalk.entities.CarbonSaving;
import pe.upc.simutalk.repositories.CarbonSavingRepository;
import pe.upc.simutalk.services.CarbonSavingCommandService;
import pe.upc.simutalk.services.CommuteDistancePolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Records the CO2e avoided by one asynchronous interview. The distance goes from the
 * candidate's district to the company's (coordinates from {@code sustainability.districts});
 * when a district is unknown, {@code sustainability.default-one-way-distance-km} is used.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CarbonSavingCommandServiceImpl implements CarbonSavingCommandService {

    private final CarbonSavingRepository carbonSavingRepository;
    private final AnalyticsExternalContextsService externalContextsService;
    private final SustainabilityProperties sustainability;

    @Override
    public Optional<CarbonSaving> handle(RecordCarbonSavingCommand command) {
        if (carbonSavingRepository.existsByApplicationId(command.applicationId())) {
            return Optional.empty();
        }
        var companyId = externalContextsService.fetchCompanyIdByJobPostingId(command.jobPostingId());
        var distance = roundTripDistance(externalContextsService.fetchCandidateDistrict(command.candidateId()),
                externalContextsService.fetchCompanyDistrict(companyId));
        var saving = CarbonSaving.compute(command.applicationId(), command.jobPostingId(), companyId, distance,
                sustainability.emissionFactorKgCo2ePerKm(), Instant.now());
        return Optional.of(carbonSavingRepository.save(saving));
    }

    private BigDecimal roundTripDistance(String candidateDistrict, String companyDistrict) {
        var from = sustainability.findDistrict(candidateDistrict);
        var to = sustainability.findDistrict(companyDistrict);
        if (from.isEmpty() || to.isEmpty()) {
            log.info("No coordinates for '{}' or '{}'; using the default distance", candidateDistrict, companyDistrict);
            return CommuteDistancePolicy.roundTripKm(sustainability.defaultOneWayDistanceKm());
        }
        return CommuteDistancePolicy.roundTripKm(
                new GeoPoint(from.get().latitude(), from.get().longitude()),
                new GeoPoint(to.get().latitude(), to.get().longitude()),
                sustainability.roadFactor());
    }
}
