package pe.upc.simutalk.securities;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Answers 403 with the standard error body when an authenticated user lacks the
 * required role at the filter-chain level.
 */
@Component
public class ForbiddenRequestHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ForbiddenRequestHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        UnauthorizedRequestHandlerEntryPoint.writeError(objectMapper, request, response, HttpStatus.FORBIDDEN,
                "Access denied");
    }
}
