package pe.upc.simutalk.exceptions;

/**
 * Thrown by an aggregate when an operation would break one of its invariants.
 * Mapped to HTTP 422.
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
