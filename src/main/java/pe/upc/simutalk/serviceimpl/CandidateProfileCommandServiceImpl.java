package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.AddCertificationCommand;
import pe.upc.simutalk.dtos.CertificationVerification;
import pe.upc.simutalk.dtos.CreateCandidateProfileCommand;
import pe.upc.simutalk.dtos.DeleteCertificationCommand;
import pe.upc.simutalk.dtos.UpdateCandidateProfileCommand;
import pe.upc.simutalk.dtos.VerifyCertificationCommand;
import pe.upc.simutalk.entities.CandidateProfile;
import pe.upc.simutalk.entities.Certification;
import pe.upc.simutalk.entities.DocumentNumber;
import pe.upc.simutalk.entities.PersonName;
import pe.upc.simutalk.exceptions.BusinessRuleViolationException;
import pe.upc.simutalk.exceptions.ResourceNotFoundException;
import pe.upc.simutalk.repositories.CandidateProfileRepository;
import pe.upc.simutalk.repositories.CompanyProfileRepository;
import pe.upc.simutalk.services.CandidateProfileCommandService;
import pe.upc.simutalk.services.CredentialVerificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Loads the candidate aggregate, delegates to it and persists. Managed aggregates are
 * written with {@code flush()} (see CLAUDE.md rule 8).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CandidateProfileCommandServiceImpl implements CandidateProfileCommandService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final ExternalIamService externalIamService;
    private final CredentialVerificationService credentialVerificationService;

    @Override
    public CandidateProfile handle(CreateCandidateProfileCommand command) {
        var documentNumber = new DocumentNumber(command.documentNumber());
        if (!externalIamService.existsUser(command.userId())) {
            throw new BusinessRuleViolationException("User %s does not exist".formatted(command.userId()));
        }
        if (candidateProfileRepository.existsByUserId(command.userId())
                || companyProfileRepository.existsByUserId(command.userId())) {
            throw new BusinessRuleViolationException("User %s already has a profile".formatted(command.userId()));
        }
        if (candidateProfileRepository.existsByDocumentNumber(documentNumber)) {
            throw new BusinessRuleViolationException("A candidate with that document number already exists");
        }
        return candidateProfileRepository.save(new CandidateProfile(command));
    }

    @Override
    public CandidateProfile handle(UpdateCandidateProfileCommand command) {
        var candidate = loadCandidate(command.candidateId());
        candidate.updateDetails(new PersonName(command.firstName(), command.lastName()), command.birthDate(),
                command.phone(), command.district(), command.yearsOfExperience(), command.email());
        candidateProfileRepository.flush();
        return candidate;
    }

    @Override
    public Certification handle(AddCertificationCommand command) {
        var candidate = loadCandidate(command.candidateId());
        var certification = candidate.addCertification(new Certification(command.title(), command.issuer(),
                command.credentialCode(), command.issuedAt(), command.expiresAt()));
        candidateProfileRepository.flush();
        return certification;
    }

    /**
     * Matched -> VERIFIED; not matched -> REJECTED; issuer unavailable -> stays UNVERIFIED.
     */
    @Override
    public CertificationVerification handle(VerifyCertificationCommand command) {
        var candidate = loadCandidate(command.candidateId());
        var certification = candidate.ensureCertificationCanBeVerified(command.certificationId());

        var result = credentialVerificationService.verify(certification.getIssuer(),
                certification.getCredentialCode(), certification.getTitle(), candidate.getPersonName().fullName());

        if (result.conclusive()) {
            if (result.matched()) {
                candidate.verifyCertification(command.certificationId(), Instant.now());
            } else {
                candidate.rejectCertification(command.certificationId());
            }
            candidateProfileRepository.flush();
        }
        return new CertificationVerification(certification, result);
    }

    @Override
    public void handle(DeleteCertificationCommand command) {
        var candidate = loadCandidate(command.candidateId());
        candidate.removeCertification(command.certificationId());
        candidateProfileRepository.flush();
    }

    private CandidateProfile loadCandidate(Long candidateId) {
        return candidateProfileRepository.findWithCertificationsById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile", candidateId));
    }
}
