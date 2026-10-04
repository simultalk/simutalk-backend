package pe.upc.simutalk.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Contact e-mail of a candidate or a company, stored lower case. Optional on both profiles. It is personal
 * data: it is used to notify the candidate and never travels to the AI provider.
 */
@Embeddable
public record EmailAddress(@Column(name = "email", length = EmailAddress.MAX_LENGTH) String value) {

    public static final int MAX_LENGTH = 254;
    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("E-mail is required");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("E-mail must be a valid address of at most %d characters".formatted(MAX_LENGTH));
        }
    }

    /** {@code null} for a missing or blank value, since the e-mail is optional. */
    public static EmailAddress ofNullable(String value) {
        return value == null || value.isBlank() ? null : new EmailAddress(value);
    }
}
