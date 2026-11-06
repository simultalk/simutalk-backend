package pe.upc.simutalk.serviceimpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class AiLanguageTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void requestWithHeader(String acceptLanguage) {
        var request = new MockHttpServletRequest();
        if (acceptLanguage != null) {
            request.addHeader("Accept-Language", acceptLanguage);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void readsEnglishFromTheHeader() {
        assertThat(AiLanguage.fromHeader("en-US")).isEqualTo(AiLanguage.ENGLISH);
        assertThat(AiLanguage.fromHeader("en-GB,en;q=0.9")).isEqualTo(AiLanguage.ENGLISH);
    }

    @Test
    void defaultsToSpanishForSpanishMissingOrUnknownValues() {
        assertThat(AiLanguage.fromHeader("es-419")).isEqualTo(AiLanguage.SPANISH);
        assertThat(AiLanguage.fromHeader("es-PE,es;q=0.9,en;q=0.8")).isEqualTo(AiLanguage.SPANISH);
        assertThat(AiLanguage.fromHeader(null)).isEqualTo(AiLanguage.SPANISH);
        assertThat(AiLanguage.fromHeader("  ")).isEqualTo(AiLanguage.SPANISH);
        assertThat(AiLanguage.fromHeader("fr-FR")).isEqualTo(AiLanguage.SPANISH);
    }

    @Test
    void usesTheHeaderOfTheCurrentRequest() {
        requestWithHeader("en-US");
        assertThat(AiLanguage.current()).isEqualTo(AiLanguage.ENGLISH);
        assertThat(AiLanguage.currentPromptInstruction()).contains("inglés");

        requestWithHeader("es-419");
        assertThat(AiLanguage.current()).isEqualTo(AiLanguage.SPANISH);
        assertThat(AiLanguage.currentPromptInstruction()).contains("español");
    }

    @Test
    void isSpanishOutsideAnHttpRequest() {
        RequestContextHolder.resetRequestAttributes();
        assertThat(AiLanguage.current()).isEqualTo(AiLanguage.SPANISH);
    }
}
