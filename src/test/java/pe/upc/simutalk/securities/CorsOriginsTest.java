package pe.upc.simutalk.securities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorsOriginsTest {

    @Test
    void anyOriginByDefault() {
        assertThat(SecurityConfiguration.originPatterns(null)).containsExactly("*");
        assertThat(SecurityConfiguration.originPatterns("  ")).containsExactly("*");
        assertThat(SecurityConfiguration.originPatterns("*")).containsExactly("*");
    }

    @Test
    void readsACommaSeparatedListWithoutTrailingSlashes() {
        assertThat(SecurityConfiguration.originPatterns(" http://localhost:4200 , http://77.112.147.229/ ,"))
                .containsExactly("http://localhost:4200", "http://77.112.147.229");
    }

    @Test
    void aWildcardInTheListAllowsAnyOrigin() {
        assertThat(SecurityConfiguration.originPatterns("http://localhost:4200,*")).containsExactly("*");
    }
}
