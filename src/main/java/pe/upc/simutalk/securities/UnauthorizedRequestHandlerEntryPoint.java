package pe.upc.simutalk.securities;

import pe.upc.simutalk.exceptions.ErrorResource;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Answers 401 with the standard {@link ErrorResource} body when a protected route is
 * called without a valid token.
 */
@Component
public class UnauthorizedRequestHandlerEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public UnauthorizedRequestHandlerEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeError(objectMapper, request, response, HttpStatus.UNAUTHORIZED,
                "A valid bearer token is required to access this resource");
    }

    static void writeError(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
                           HttpStatus status, String message) throws IOException {
        var body = new ErrorResource(Instant.now(), status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
