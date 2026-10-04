package pe.upc.simutalk.exceptions;

/**
 * Thrown by an aggregate when a requested state transition is not allowed.
 * It is an {@link IllegalStateException}; the API maps it to HTTP 422.
 */
public class InvalidStateTransitionException extends IllegalStateException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
