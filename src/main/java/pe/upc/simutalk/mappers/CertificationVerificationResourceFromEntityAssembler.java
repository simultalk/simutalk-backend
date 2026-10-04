package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.CertificationVerification;
import pe.upc.simutalk.dtos.CertificationVerificationResource;

public class CertificationVerificationResourceFromEntityAssembler {

    public static CertificationVerificationResource toResourceFromEntity(CertificationVerification verification) {
        var result = verification.result();
        return new CertificationVerificationResource(
                CertificationResourceFromEntityAssembler.toResourceFromEntity(verification.certification()),
                result.matched(), result.conclusive(), result.detail());
    }
}
