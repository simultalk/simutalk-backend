package pe.upc.simutalk.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Error body returned by every endpoint of the API.
 *
 * @param timestamp moment the error was produced (UTC)
 * @param status    HTTP status code
 * @param error     HTTP reason phrase
 * @param message   human readable explanation
 * @param path      request path
 * @param details   field level validation errors, omitted when empty
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResource(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorResource> details) {

    public record FieldErrorResource(String field, String message) {
    }
}
