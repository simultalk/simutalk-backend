package pe.upc.simutalk.exceptions;

/**
 * Thrown when a requested resource does not exist. Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super("%s with id %s not found".formatted(resourceName, id));
    }
}
