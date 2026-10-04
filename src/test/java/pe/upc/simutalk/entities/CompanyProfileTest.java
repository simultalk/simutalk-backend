package pe.upc.simutalk.entities;

import pe.upc.simutalk.dtos.CreateCompanyProfileCommand;
import pe.upc.simutalk.enums.CompanySize;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyProfileTest {

    private static CreateCompanyProfileCommand command(String ruc) {
        return new CreateCompanyProfileCommand(5L, "Consultora Andina S.A.C.", "Consultora Andina",
                "Consultoría de TI", ruc, CompanySize.MEDIANA, "San Isidro");
    }

    @ParameterizedTest
    @ValueSource(strings = {"20554873621", "10456789012", " 20554873621 "})
    void acceptsValidRuc(String ruc) {
        assertThat(new CompanyProfile(command(ruc)).getRuc().value()).hasSize(11);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "2055487362", "205548736211", "30554873621", "15554873621", "20A54873621"})
    void rejectsInvalidRuc(String ruc) {
        assertThatThrownBy(() -> new CompanyProfile(command(ruc)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RUC");
    }

    @Test
    void requiresCompanySizeAndPositiveUserId() {
        assertThatThrownBy(() -> new CompanyProfile(new CreateCompanyProfileCommand(5L, "A", "B", "C",
                "20554873621", null, "Miraflores"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompanyProfile(new CreateCompanyProfileCommand(0L, "A", "B", "C",
                "20554873621", CompanySize.MICRO, "Miraflores"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theEmailIsOptionalValidatedAndEditable() {
        var company = new CompanyProfile(new CreateCompanyProfileCommand(5L, "Consultora Andina S.A.C.", "Andina",
                "TI", "20554873621", CompanySize.MEDIANA, "San Isidro", "Seleccion@Andina.example.com"));
        assertThat(company.getEmail().value()).isEqualTo("seleccion@andina.example.com");

        company.updateDetails("Consultora Andina S.A.C.", "Andina", "TI", CompanySize.MEDIANA, "San Isidro", null);
        assertThat(company.getEmail()).isNull();
        assertThatThrownBy(() -> new CompanyProfile(new CreateCompanyProfileCommand(5L, "A", "B", "C", "20554873621",
                CompanySize.MICRO, "Miraflores", "no-es-correo"))).isInstanceOf(IllegalArgumentException.class);
    }
}
