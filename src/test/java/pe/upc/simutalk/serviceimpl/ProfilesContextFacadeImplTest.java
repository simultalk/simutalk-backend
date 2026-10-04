package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CandidateContact;
import pe.upc.simutalk.dtos.CreateCandidateProfileCommand;
import pe.upc.simutalk.dtos.CreateCompanyProfileCommand;
import pe.upc.simutalk.dtos.GetCandidateProfileByIdQuery;
import pe.upc.simutalk.dtos.GetCandidateProfileByUserIdQuery;
import pe.upc.simutalk.dtos.GetCompanyProfileByIdQuery;
import pe.upc.simutalk.dtos.GetCompanyProfileByUserIdQuery;
import pe.upc.simutalk.dtos.GetVerifiedCertificationCountQuery;
import pe.upc.simutalk.entities.CandidateProfile;
import pe.upc.simutalk.entities.CompanyProfile;
import pe.upc.simutalk.enums.CompanySize;
import pe.upc.simutalk.services.CandidateProfileQueryService;
import pe.upc.simutalk.services.CompanyProfileQueryService;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfilesContextFacadeImplTest {

    private final CandidateProfileQueryService candidates = mock(CandidateProfileQueryService.class);
    private final CompanyProfileQueryService companies = mock(CompanyProfileQueryService.class);
    private final ProfilesContextFacadeImpl facade = new ProfilesContextFacadeImpl(candidates, companies);

    @Test
    void resolvesCandidateData() {
        var candidate = new CandidateProfile(new CreateCandidateProfileCommand(10L, "Rosa", "Quispe", "45879632",
                LocalDate.now().minusYears(25), "+51987654321", "Comas", 2));
        ReflectionTestUtils.setField(candidate, "id", 3L);
        when(candidates.handle(new GetCandidateProfileByUserIdQuery(10L))).thenReturn(Optional.of(candidate));
        when(candidates.handle(new GetCandidateProfileByIdQuery(3L))).thenReturn(Optional.of(candidate));
        when(candidates.handle(new GetVerifiedCertificationCountQuery(3L))).thenReturn(2L);

        assertThat(facade.fetchCandidateIdByUserId(10L)).isEqualTo(3L);
        assertThat(facade.fetchCandidateDistrict(3L)).isEqualTo("Comas");
        assertThat(facade.fetchVerifiedCertificationCount(3L)).isEqualTo(2L);
    }

    @Test
    void returnsNeutralValuesWhenMissing() {
        when(candidates.handle(any(GetCandidateProfileByUserIdQuery.class))).thenReturn(Optional.empty());
        when(candidates.handle(any(GetCandidateProfileByIdQuery.class))).thenReturn(Optional.empty());
        when(companies.handle(any(GetCompanyProfileByUserIdQuery.class))).thenReturn(Optional.empty());

        assertThat(facade.fetchCandidateIdByUserId(99L)).isZero();
        assertThat(facade.fetchCompanyIdByUserId(99L)).isZero();
        assertThat(facade.fetchCandidateDistrict(99L)).isEmpty();
        assertThat(facade.fetchVerifiedCertificationCount(null)).isZero();
    }

    @Test
    void exposesTheEmailsForNotificationsAndRedaction() {
        var candidate = new CandidateProfile(new CreateCandidateProfileCommand(10L, "Rosa", "Quispe", "45879632",
                LocalDate.now().minusYears(25), "+51987654321", "Comas", 2, "rosa@example.com"));
        ReflectionTestUtils.setField(candidate, "id", 3L);
        var company = new CompanyProfile(new CreateCompanyProfileCommand(20L, "Andina S.A.C.", "Andina", "TI",
                "20554873621", CompanySize.MEDIANA, "San Isidro", "seleccion@andina.example.com"));
        when(candidates.handle(new GetCandidateProfileByIdQuery(3L))).thenReturn(Optional.of(candidate));
        when(companies.handle(new GetCompanyProfileByIdQuery(1L))).thenReturn(Optional.of(company));
        when(companies.handle(new GetCompanyProfileByIdQuery(2L))).thenReturn(Optional.empty());

        assertThat(facade.fetchCandidateContact(3L)).isEqualTo(new CandidateContact(3L, "Rosa", "Rosa Quispe", "rosa@example.com"));
        assertThat(facade.fetchCandidatePersonalData(3L).email()).isEqualTo("rosa@example.com");
        assertThat(facade.fetchCompanyEmail(1L)).isEqualTo("seleccion@andina.example.com");
        assertThat(facade.fetchCompanyEmail(2L)).isEmpty();
    }
}
