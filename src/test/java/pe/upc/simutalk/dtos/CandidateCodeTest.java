package pe.upc.simutalk.dtos;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateCodeTest {

    private static final String SECRET = "test-secret";

    @Test
    void hasTheExpectedFormat() {
        assertThat(CandidateCode.of(7L, 10L, SECRET).value()).matches("CANDIDATO-[A-Z]-\\d{4}");
    }

    @Test
    void isStableForTheSameCandidateAndJobPosting() {
        assertThat(CandidateCode.of(7L, 10L, SECRET)).isEqualTo(CandidateCode.of(7L, 10L, SECRET));
    }

    @Test
    void changesBetweenJobPostingsAndSecrets() {
        var base = CandidateCode.of(7L, 10L, SECRET);

        assertThat(CandidateCode.of(7L, 11L, SECRET)).isNotEqualTo(base);
        assertThat(CandidateCode.of(7L, 10L, "other-secret")).isNotEqualTo(base);
    }

    @Test
    void rejectsInvalidValues() {
        assertThatThrownBy(() -> new CandidateCode("CANDIDATO-7")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CandidateCode.of(7L, 10L, " ")).isInstanceOf(IllegalArgumentException.class);
    }
}
