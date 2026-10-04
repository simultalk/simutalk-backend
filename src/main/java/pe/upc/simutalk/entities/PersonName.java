package pe.upc.simutalk.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record PersonName(
        @Column(name = "first_name", nullable = false, length = 80) String firstName,
        @Column(name = "last_name", nullable = false, length = 80) String lastName) {

    public static final int MAX_LENGTH = 80;

    public PersonName {
        firstName = requireName(firstName, "First name");
        lastName = requireName(lastName, "Last name");
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    private static String requireName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        var stripped = value.strip();
        if (stripped.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("%s must be at most %d characters".formatted(field, MAX_LENGTH));
        }
        return stripped;
    }
}
