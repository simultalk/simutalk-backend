package pe.upc.simutalk.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAddressTest {

    @Test
    void isStoredLowerCaseAndTrimmed() {
        assertThat(new EmailAddress("  Rosa.Quispe@Example.COM ").value()).isEqualTo("rosa.quispe@example.com");
    }

    @Test
    void rejectsMalformedAddresses() {
        for (var invalid : new String[]{"rosa", "rosa@", "@example.com", "rosa@example", "ro sa@example.com", "rosa@@example.com"}) {
            assertThatThrownBy(() -> new EmailAddress(invalid)).as(invalid).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new EmailAddress("a".repeat(250) + "@example.com")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isOptional() {
        assertThat(EmailAddress.ofNullable(null)).isNull();
        assertThat(EmailAddress.ofNullable("  ")).isNull();
        assertThat(EmailAddress.ofNullable("r@example.com")).isEqualTo(new EmailAddress("r@example.com"));
    }
}
