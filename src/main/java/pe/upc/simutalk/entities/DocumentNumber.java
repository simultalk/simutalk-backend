package pe.upc.simutalk.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Identity document of a candidate: DNI (8 digits) or carné de extranjería (9 to 12 digits).
 */
@Embeddable
public record DocumentNumber(@Column(name = "document_number", nullable = false, unique = true, length = 12) String value) {

    public DocumentNumber {
        if (value == null || !value.strip().matches("\\d{8}|\\d{9,12}")) {
            throw new IllegalArgumentException(
                    "Document number must be a DNI (8 digits) or a carné de extranjería (9 to 12 digits)");
        }
        value = value.strip();
    }

    public String documentType() {
        return value.length() == 8 ? "DNI" : "CE";
    }
}
