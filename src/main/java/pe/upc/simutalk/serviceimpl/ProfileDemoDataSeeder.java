package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.AddCertificationCommand;
import pe.upc.simutalk.dtos.CreateCandidateProfileCommand;
import pe.upc.simutalk.dtos.CreateCompanyProfileCommand;
import pe.upc.simutalk.dtos.VerifyCertificationCommand;
import pe.upc.simutalk.enums.CompanySize;
import pe.upc.simutalk.repositories.CandidateProfileRepository;
import pe.upc.simutalk.repositories.CompanyProfileRepository;
import pe.upc.simutalk.services.CandidateProfileCommandService;
import pe.upc.simutalk.services.CompanyProfileCommandService;
import pe.upc.simutalk.services.IamContextFacade;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Seeds demonstration data when {@code app.seed-demo-data=true} and the profile tables
 * are empty: one company and six candidates with certifications in every verification
 * state. Everything goes through the command services, so the domain rules apply.
 * Idempotent: once there are profiles it does nothing.
 */
@Slf4j
@Service
public class ProfileDemoDataSeeder {

    private static final String COMPANY_USERNAME = "consultora.andina";
    /** Reserved domain (RFC 2606): demo notifications can never reach a real inbox. */
    private static final String DEMO_EMAIL_DOMAIN = "@example.com";

    private final boolean enabled;
    private final String demoPassword;
    private final IamContextFacade iamContextFacade;
    private final CompanyProfileCommandService companyProfileCommandService;
    private final CandidateProfileCommandService candidateProfileCommandService;
    private final CompanyProfileRepository companyProfileRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final TransactionTemplate transactionTemplate;

    public ProfileDemoDataSeeder(@Value("${app.seed-demo-data:false}") boolean enabled,
                          @Value("${app.demo-users-password:}") String demoPassword,
                          IamContextFacade iamContextFacade,
                          CompanyProfileCommandService companyProfileCommandService,
                          CandidateProfileCommandService candidateProfileCommandService,
                          CompanyProfileRepository companyProfileRepository,
                          CandidateProfileRepository candidateProfileRepository,
                          TransactionTemplate transactionTemplate) {
        this.enabled = enabled;
        this.demoPassword = demoPassword;
        this.iamContextFacade = iamContextFacade;
        this.companyProfileCommandService = companyProfileCommandService;
        this.candidateProfileCommandService = candidateProfileCommandService;
        this.companyProfileRepository = companyProfileRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.transactionTemplate = transactionTemplate;
    }

    /** Runs after iam has seeded its roles. */
    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void on(ApplicationReadyEvent event) {
        // Demo data must never keep the application from starting.
        try {
            seedProfiles();
        } catch (RuntimeException ex) {
            log.warn("Demo data: the demo company and candidate profiles could not be seeded; the application starts without them", ex);
        }
    }

    private void seedProfiles() {
        if (!enabled) {
            return;
        }
        if (companyProfileRepository.count() > 0 || candidateProfileRepository.count() > 0) {
            log.info("Demo data skipped: profiles already exist");
            return;
        }
        var password = resolvePassword();
        transactionTemplate.executeWithoutResult(status -> seed(password));
        log.info("Demo data seeded: 1 company and {} candidates", DEMO_CANDIDATES.size());
    }

    private void seed(String password) {
        var companyUserId = iamContextFacade.signUpUserIfAbsent(COMPANY_USERNAME, password, "ROLE_RECRUITER");
        companyProfileCommandService.handle(new CreateCompanyProfileCommand(companyUserId,
                "Consultora Andina S.A.C.", "Consultora Andina", "Consultoría de TI", "20554873621",
                CompanySize.MEDIANA, "San Isidro", COMPANY_USERNAME + DEMO_EMAIL_DOMAIN));

        for (var demo : DEMO_CANDIDATES) {
            var userId = iamContextFacade.signUpUserIfAbsent(demo.username(), password, "ROLE_CANDIDATE");
            var candidate = candidateProfileCommandService.handle(new CreateCandidateProfileCommand(userId,
                    demo.firstName(), demo.lastName(), demo.documentNumber(), demo.birthDate(), demo.phone(),
                    demo.district(), demo.yearsOfExperience(), demo.username() + DEMO_EMAIL_DOMAIN));
            for (var cert : demo.certifications()) {
                var certification = candidateProfileCommandService.handle(new AddCertificationCommand(
                        candidate.getId(), cert.title(), cert.issuer(), cert.credentialCode(), cert.issuedAt(),
                        cert.expiresAt()));
                if (cert.requestVerification()) {
                    candidateProfileCommandService.handle(
                            new VerifyCertificationCommand(candidate.getId(), certification.getId()));
                }
            }
        }
    }

    private String resolvePassword() {
        if (demoPassword != null && !demoPassword.isBlank()) {
            return demoPassword;
        }
        log.warn("DEMO_USERS_PASSWORD is not set: demo users get a random password and cannot sign in");
        return UUID.randomUUID().toString();
    }

    private record DemoCertification(String title, String issuer, String credentialCode, LocalDate issuedAt,
                                     LocalDate expiresAt, boolean requestVerification) {
    }

    private record DemoCandidate(String username, String firstName, String lastName, String documentNumber,
                                 LocalDate birthDate, String phone, String district, int yearsOfExperience,
                                 List<DemoCertification> certifications) {
    }

    /*
     * With external.credentials.mode=mock: codes of 8+ characters end VERIFIED, shorter ones
     * REJECTED, a certification without code stays UNVERIFIED, and one is left unrequested.
     */
    private static final List<DemoCandidate> DEMO_CANDIDATES = List.of(
            new DemoCandidate("rosa.quispe", "Rosa", "Quispe Mamani", "45879632", LocalDate.of(1996, 3, 14),
                    "+51987654321", "San Juan de Lurigancho", 4, List.of(
                    new DemoCertification("Google Data Analytics", "Coursera", "COURSERA-7XK29QPL",
                            LocalDate.of(2024, 5, 20), null, true),
                    new DemoCertification("AWS Certified Cloud Practitioner", "Credly", "CREDLY-AWS-55820913",
                            LocalDate.of(2025, 2, 11), LocalDate.of(2028, 2, 11), true))),
            new DemoCandidate("jorge.huaman", "Jorge", "Huamán Torres", "70214589", LocalDate.of(1993, 11, 2),
                    "+51912345678", "Comas", 7, List.of(
                    new DemoCertification("Scrum Foundation Professional Certificate", "CertiProf", null,
                            LocalDate.of(2023, 8, 3), null, false),
                    new DemoCertification("IBM Data Science Professional Certificate", "Coursera", "IBMDS-4411902",
                            LocalDate.of(2022, 10, 15), null, true),
                    new DemoCertification("Professional Scrum Master I", "Credly", "PSM-123",
                            LocalDate.of(2024, 1, 9), null, true))),
            new DemoCandidate("lucia.flores", "Lucía", "Flores Chávez", "46325871", LocalDate.of(1999, 7, 21),
                    "+51923456789", "Miraflores", 2, List.of()),
            new DemoCandidate("miguel.condori", "Miguel", "Condori Apaza", "72583146", LocalDate.of(2000, 1, 30),
                    "+51934567890", "Villa El Salvador", 1, List.of(
                    new DemoCertification("Design Thinking Professional Certificate", "CertiProf",
                            "CERTIPROF-DTPC-88213", LocalDate.of(2025, 4, 2), null, true))),
            new DemoCandidate("carmen.ramos", "Carmen", "Ramos Villanueva", "41259873", LocalDate.of(1990, 5, 9),
                    "+51945678901", "Santiago de Surco", 10, List.of(
                    new DemoCertification("Google Project Management", "Coursera", "COURSERA-PM-99021",
                            LocalDate.of(2021, 6, 1), LocalDate.of(2024, 12, 31), true),
                    new DemoCertification("ITIL 4 Foundation", "Credly", "ITIL4-FND-66102",
                            LocalDate.of(2023, 3, 17), null, false))),
            new DemoCandidate("diego.salazar", "Diego", "Salazar Rojas", "001234567", LocalDate.of(1995, 9, 18),
                    "+51956789012", "San Miguel", 5, List.of(
                    new DemoCertification("Microsoft Certified: Azure Fundamentals", "Credly", "AZ900-77310245",
                            LocalDate.of(2024, 9, 5), null, true)))
    );
}
