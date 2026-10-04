package pe.upc.simutalk.dtos;

import pe.upc.simutalk.enums.CompanySize;

public record CreateCompanyProfileCommand(
        Long userId,
        String legalName,
        String tradeName,
        String industry,
        String ruc,
        CompanySize companySize,
        String district,
        String email) {

    /** A profile without e-mail. */
    public CreateCompanyProfileCommand(Long userId, String legalName, String tradeName, String industry, String ruc,
                                       CompanySize companySize, String district) {
        this(userId, legalName, tradeName, industry, ruc, companySize, district, null);
    }
}
