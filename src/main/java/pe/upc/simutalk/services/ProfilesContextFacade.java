package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CandidateContact;
import pe.upc.simutalk.dtos.CandidatePersonalData;

/**
 * Contract other bounded contexts (e.g. assessment, interviews) use to ask profiles
 * about candidates and companies. Implemented by profiles. No scoring logic here:
 * how much a certification is worth is decided by the caller.
 */
public interface ProfilesContextFacade {

    /**
     * @return the candidate profile id, or {@code 0L} if the user has none
     */
    Long fetchCandidateIdByUserId(Long userId);

    /**
     * @return the company profile id, or {@code 0L} if the user has none
     */
    Long fetchCompanyIdByUserId(Long userId);

    /** @return the company's district, or an empty string if the company does not exist */
    String fetchCompanyDistrict(Long companyId);

    /**
     * @return certifications that are VERIFIED and not expired today; 0 if the candidate does not exist
     */
    long fetchVerifiedCertificationCount(Long candidateId);

    /**
     * @return the candidate district, or an empty string if the candidate does not exist
     */
    String fetchCandidateDistrict(Long candidateId);

    /** Declared certifications the issuer did not match (REJECTED); 0 if the candidate does not exist. */
    long fetchRejectedCertificationCount(Long candidateId);

    /**
     * PII of the candidate, for redaction and for authorized display only.
     *
     * @return the data, or {@code null} if the candidate does not exist
     */
    CandidatePersonalData fetchCandidatePersonalData(Long candidateId);

    /**
     * Name and e-mail of the candidate, to notify them.
     *
     * @return the contact ({@code email} may be {@code null}), or {@code null} if the candidate does not exist
     */
    CandidateContact fetchCandidateContact(Long candidateId);

    /** @return the company's contact e-mail, or an empty string if it has none or does not exist */
    String fetchCompanyEmail(Long companyId);
}
