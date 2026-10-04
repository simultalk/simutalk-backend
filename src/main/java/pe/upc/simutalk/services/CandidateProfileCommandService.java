package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.AddCertificationCommand;
import pe.upc.simutalk.dtos.CertificationVerification;
import pe.upc.simutalk.dtos.CreateCandidateProfileCommand;
import pe.upc.simutalk.dtos.DeleteCertificationCommand;
import pe.upc.simutalk.dtos.UpdateCandidateProfileCommand;
import pe.upc.simutalk.dtos.VerifyCertificationCommand;
import pe.upc.simutalk.entities.CandidateProfile;
import pe.upc.simutalk.entities.Certification;

public interface CandidateProfileCommandService {

    CandidateProfile handle(CreateCandidateProfileCommand command);

    CandidateProfile handle(UpdateCandidateProfileCommand command);

    Certification handle(AddCertificationCommand command);

    CertificationVerification handle(VerifyCertificationCommand command);

    void handle(DeleteCertificationCommand command);
}
