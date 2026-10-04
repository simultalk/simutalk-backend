package pe.upc.simutalk.controllers;

import pe.upc.simutalk.dtos.CreateCandidateProfileCommand;
import pe.upc.simutalk.dtos.CreateCompanyProfileCommand;
import pe.upc.simutalk.dtos.GetAllCandidateProfilesQuery;
import pe.upc.simutalk.dtos.GetAllCompanyProfilesQuery;
import pe.upc.simutalk.dtos.GetCandidateProfileByIdQuery;
import pe.upc.simutalk.dtos.GetCandidateProfileByUserIdQuery;
import pe.upc.simutalk.dtos.GetCompanyProfileByUserIdQuery;
import pe.upc.simutalk.entities.CandidateProfile;
import pe.upc.simutalk.entities.CompanyProfile;
import pe.upc.simutalk.enums.CompanySize;
import pe.upc.simutalk.exceptions.GlobalExceptionHandler;
import pe.upc.simutalk.securities.ProfileAccessPolicy;
import pe.upc.simutalk.services.CandidateProfileCommandService;
import pe.upc.simutalk.services.CandidateProfileQueryService;
import pe.upc.simutalk.services.CompanyProfileCommandService;
import pe.upc.simutalk.services.CompanyProfileQueryService;
import pe.upc.simutalk.services.IamContextFacade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real routing (MockMvc over the actual controllers), method security and error handling for
 * the profile read endpoints, without web server or database. Security filters are not part of
 * this context: the authenticated user comes from {@code @WithMockUser}.
 */
@SpringJUnitWebConfig(ProfileSelfEndpointsTest.Config.class)
class ProfileSelfEndpointsTest {

    @Configuration
    @EnableWebMvc
    @EnableMethodSecurity
    static class Config {
        @Bean static MethodValidationPostProcessor methodValidationPostProcessor() { return new MethodValidationPostProcessor(); }
        @Bean IamContextFacade iamContextFacade() { return mock(IamContextFacade.class); }
        @Bean CandidateProfileQueryService candidateProfileQueryService() { return mock(CandidateProfileQueryService.class); }
        @Bean CandidateProfileCommandService candidateProfileCommandService() { return mock(CandidateProfileCommandService.class); }
        @Bean CompanyProfileQueryService companyProfileQueryService() { return mock(CompanyProfileQueryService.class); }
        @Bean CompanyProfileCommandService companyProfileCommandService() { return mock(CompanyProfileCommandService.class); }
        @Bean GlobalExceptionHandler globalExceptionHandler() { return new GlobalExceptionHandler(); }

        @Bean(name = "profileAccess")
        ProfileAccessPolicy profileAccess(IamContextFacade iam, CandidateProfileQueryService candidates,
                                          CompanyProfileQueryService companies) {
            return new ProfileAccessPolicy(iam, candidates, companies);
        }

        @Bean CandidateProfileController candidateProfilesController(CandidateProfileCommandService commands,
                                                                      CandidateProfileQueryService queries,
                                                                      ProfileAccessPolicy policy) {
            return new CandidateProfileController(commands, queries, policy);
        }

        @Bean CompanyProfileController companyProfilesController(CompanyProfileCommandService commands,
                                                                  CompanyProfileQueryService queries,
                                                                  ProfileAccessPolicy policy) {
            return new CompanyProfileController(commands, queries, policy);
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired IamContextFacade iam;
    @Autowired CandidateProfileQueryService candidates;
    @Autowired CompanyProfileQueryService companies;

    private MockMvc mvc;
    private CandidateProfile rosa;
    private CompanyProfile andina;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        reset(iam, candidates, companies);
        rosa = new CandidateProfile(new CreateCandidateProfileCommand(4L, "Rosa", "Quispe", "45879632",
                LocalDate.now().minusYears(25), "+51987654321", "Comas", 3));
        ReflectionTestUtils.setField(rosa, "id", 1L);
        andina = new CompanyProfile(new CreateCompanyProfileCommand(3L, "Consultora Andina S.A.C.",
                "Consultora Andina", "TI", "20554873621", CompanySize.MEDIANA, "San Isidro"));
        ReflectionTestUtils.setField(andina, "id", 1L);

        when(iam.fetchUserIdByUsername("rosa")).thenReturn(4L);
        when(iam.fetchUserIdByUsername("andina")).thenReturn(3L);
        when(iam.fetchUserIdByUsername("nuevo")).thenReturn(9L);
        when(iam.fetchUserIdByUsername("admin")).thenReturn(1L);
        when(candidates.handle(any(GetCandidateProfileByUserIdQuery.class))).thenReturn(Optional.empty());
        when(candidates.handle(new GetCandidateProfileByUserIdQuery(4L))).thenReturn(Optional.of(rosa));
        when(candidates.handle(any(GetCandidateProfileByIdQuery.class))).thenReturn(Optional.of(rosa));
        when(companies.handle(any(GetCompanyProfileByUserIdQuery.class))).thenReturn(Optional.empty());
        when(companies.handle(new GetCompanyProfileByUserIdQuery(3L))).thenReturn(Optional.of(andina));
        when(candidates.handle(any(GetAllCandidateProfilesQuery.class)))
                .thenReturn(new PageImpl<>(List.of(rosa), PageRequest.of(0, 5), 11));
        when(companies.handle(any(GetAllCompanyProfilesQuery.class)))
                .thenReturn(new PageImpl<>(List.of(andina), PageRequest.of(0, 20), 1));
    }

    @Test
    @WithMockUser(username = "rosa", roles = "CANDIDATE")
    void meReturnsTheAuthenticatedCandidateProfile() throws Exception {
        mvc.perform(get("/api/v1/candidate-profiles/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(4));
    }

    @Test
    @WithMockUser(username = "nuevo", roles = "CANDIDATE")
    void meWithoutProfileIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/candidate-profiles/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The authenticated user has no candidate profile"));
        mvc.perform(get("/api/v1/company-profiles/me")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "andina", roles = "RECRUITER")
    void companyMeReturnsTheAuthenticatedCompanyProfile() throws Exception {
        mvc.perform(get("/api/v1/company-profiles/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").value("20554873621"));
    }

    @Test
    @WithMockUser(username = "andina", roles = "RECRUITER")
    void byIdStillWorksAndMeIsNotParsedAsAnId() throws Exception {
        mvc.perform(get("/api/v1/candidate-profiles/1")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/candidate-profiles/me")).andExpect(status().isNotFound());
        verify(candidates, never()).handle(new GetCandidateProfileByIdQuery(null));
    }

    @Test
    @WithMockUser(username = "rosa", roles = "CANDIDATE")
    void oldUserIdQueryParamNoLongerSelectsAProfile() throws Exception {
        mvc.perform(get("/api/v1/candidate-profiles").param("userId", "4")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminListsCandidatesPaginated() throws Exception {
        mvc.perform(get("/api/v1/candidate-profiles").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(3));
        verify(candidates).handle(new GetAllCandidateProfilesQuery(0, 5));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminListsCompaniesWithDefaultPaging() throws Exception {
        mvc.perform(get("/api/v1/company-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tradeName").value("Consultora Andina"));
        verify(companies).handle(new GetAllCompanyProfilesQuery(0, 20));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void invalidPagingIsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/candidate-profiles").param("size", "500")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/company-profiles").param("page", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "andina", roles = "RECRUITER")
    void listingIsAdminOnly() throws Exception {
        mvc.perform(get("/api/v1/candidate-profiles")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/company-profiles")).andExpect(status().isForbidden());
    }
}
