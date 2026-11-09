package pe.upc.simutalk.serviceimpl;

import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;

/**
 * Language the generative AI must answer in, taken from the {@code Accept-Language} header of the current
 * HTTP request. Supported values: {@code es-419} (default) and {@code en-US}.
 * <p>
 * Outside an HTTP request (for instance, when an interview is scored after it is completed) there is no
 * header, so Spanish is used. The mock implementations always answer in Spanish; the frontend translates them.
 */
public final class AiLanguage {

    public static final String SPANISH = "es-419";
    public static final String ENGLISH = "en-US";

    private AiLanguage() {
    }

    /** Language requested by the current HTTP request; {@link #SPANISH} when there is none. */
    public static String current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return SPANISH;
        }
        return fromHeader(attributes.getRequest().getHeader(HttpHeaders.ACCEPT_LANGUAGE));
    }

    /** Reads the first language of an {@code Accept-Language} value: English if it starts with "en", else Spanish. */
    static String fromHeader(String header) {
        if (header == null || header.isBlank()) {
            return SPANISH;
        }
        var first = header.split(",")[0].split(";")[0].strip().toLowerCase(Locale.ROOT);
        return first.startsWith("en") ? ENGLISH : SPANISH;
    }

    /** Line appended to a Gemini prompt so the texts come back in the requested language. */
    public static String promptInstruction(String language) {
        return ENGLISH.equals(language)
                ? "IDIOMA DE RESPUESTA: escribe todos los textos en inglés (en-US). Las claves del JSON no se traducen."
                : "IDIOMA DE RESPUESTA: escribe todos los textos en español latinoamericano (es-419).";
    }

    /** {@link #promptInstruction(String)} for the language of the current request. */
    public static String currentPromptInstruction() {
        return promptInstruction(current());
    }
}
