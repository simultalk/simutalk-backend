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

    /**
     * Language instruction for a Gemini prompt. The prompts and the inputs are in Spanish, so the English
     * instruction is written in English and is explicit: a weaker one was ignored by the lite models.
     * Fragments that must be copied literally from the input are never translated.
     */
    public static String promptInstruction(String language) {
        return ENGLISH.equals(language)
                ? "IDIOMA DE RESPUESTA (inglés, en-US) / RESPONSE LANGUAGE: write EVERY text value of the JSON in "
                        + "English (en-US), even though these instructions and the input are in Spanish. Do not translate "
                        + "the JSON keys. A fragment that must be copied literally from the input (such as \"excerpt\") "
                        + "stays exactly as it appears in the input."
                : "IDIOMA DE RESPUESTA: escribe todos los textos en español latinoamericano (es-419). Los fragmentos que "
                        + "se copian literalmente de la entrada (como \"excerpt\") se dejan tal como aparecen en ella.";
    }

    /** {@link #promptInstruction(String)} for the language of the current request. */
    public static String currentPromptInstruction() {
        return promptInstruction(current());
    }

    /**
     * The prompt with the language instruction of the current request before and after it: models follow an
     * instruction more reliably when it frames the prompt than when it only closes it.
     */
    public static String inCurrentLanguage(String prompt) {
        var instruction = currentPromptInstruction();
        return instruction + "\n\n" + prompt + "\n" + instruction;
    }
}
