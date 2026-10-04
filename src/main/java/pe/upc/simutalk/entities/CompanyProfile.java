package pe.upc.simutalk.entities;

import pe.upc.simutalk.dtos.CreateCompanyProfileCommand;
import pe.upc.simutalk.enums.CompanySize;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Profile of a company that publishes job postings. Owned by a user of the iam
 * context, referenced only by {@code userId}. The RUC cannot change once created.
 */
@Getter
@Entity
@Table(name = "companies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyProfile extends AuditableAggregateRoot<CompanyProfile> {

    public static final int TEXT_MAX_LENGTH = 150;
    public static final int DISTRICT_MAX_LENGTH = 80;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "legal_name", nullable = false, length = TEXT_MAX_LENGTH)
    private String legalName;

    @Column(name = "trade_name", nullable = false, length = TEXT_MAX_LENGTH)
    private String tradeName;

    @Column(nullable = false, length = TEXT_MAX_LENGTH)
    private String industry;

    @Embedded
    private Ruc ruc;

    /** Optional contact e-mail. */
    @Embedded
    private EmailAddress email;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_size", nullable = false, length = 20)
    private CompanySize companySize;

    @Column(nullable = false, length = DISTRICT_MAX_LENGTH)
    private String district;

    public CompanyProfile(CreateCompanyProfileCommand command) {
        if (command.userId() == null || command.userId() <= 0) {
            throw new IllegalArgumentException("User id must be a positive number");
        }
        this.userId = command.userId();
        this.ruc = new Ruc(command.ruc());
        applyDetails(command.legalName(), command.tradeName(), command.industry(), command.companySize(),
                command.district());
        this.email = EmailAddress.ofNullable(command.email());
    }

    /** Changes the editable data; the e-mail is kept as it is. */
    public void updateDetails(String legalName, String tradeName, String industry, CompanySize companySize,
                              String district) {
        applyDetails(legalName, tradeName, industry, companySize, district);
    }

    /** Changes the editable data, including the e-mail ({@code null} or blank removes it). */
    public void updateDetails(String legalName, String tradeName, String industry, CompanySize companySize,
                              String district, String email) {
        applyDetails(legalName, tradeName, industry, companySize, district);
        this.email = EmailAddress.ofNullable(email);
    }

    public boolean isOwnedBy(Long otherUserId) {
        return userId.equals(otherUserId);
    }

    private void applyDetails(String legalName, String tradeName, String industry, CompanySize companySize,
                              String district) {
        if (companySize == null) {
            throw new IllegalArgumentException("Company size is required");
        }
        this.legalName = requireText(legalName, "legal name", TEXT_MAX_LENGTH);
        this.tradeName = requireText(tradeName, "trade name", TEXT_MAX_LENGTH);
        this.industry = requireText(industry, "industry", TEXT_MAX_LENGTH);
        this.district = requireText(district, "district", DISTRICT_MAX_LENGTH);
        this.companySize = companySize;
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Company %s is required".formatted(field));
        }
        var stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new IllegalArgumentException("Company %s must be at most %d characters".formatted(field, maxLength));
        }
        return stripped;
    }
}
