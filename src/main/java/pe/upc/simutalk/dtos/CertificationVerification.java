package pe.upc.simutalk.dtos;

import pe.upc.simutalk.services.CredentialVerificationService.VerificationResult;

import pe.upc.simutalk.entities.Certification;

/**
 * Outcome of a verification request: the certification in its resulting state and
 * what the issuer answered.
 */
public record CertificationVerification(Certification certification, VerificationResult result) {
}
