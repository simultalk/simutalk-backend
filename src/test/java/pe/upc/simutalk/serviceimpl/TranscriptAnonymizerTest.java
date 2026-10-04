package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CandidatePersonalData;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptAnonymizerTest {

    private final TranscriptAnonymizer anonymizer = new TranscriptAnonymizer();
    private final CandidatePersonalData rosa = new CandidatePersonalData(7L, "Rosa", "Quispe Mamani", "45879632",
            "+51987654321", "San Juan de Lurigancho", LocalDate.of(1996, 3, 14));

    @Test
    void redactsTheCandidatesOwnData() {
        var original = "Hola, soy Rosa Quispe, mi DNI es 45879632 y vivo en San Juan de Lurigancho. "
                + "Me pueden llamar al 987 654 321 o escribir a rosa.q@mail.com. Tengo 30 años.";

        var text = anonymizer.anonymize(original, rosa).text();

        assertThat(text).doesNotContain("Rosa", "Quispe", "45879632", "San Juan de Lurigancho", "987 654 321",
                "rosa.q@mail.com", "30 años");
        assertThat(text).contains("[NOMBRE]", "[DOCUMENTO]", "[DIRECCION]", "[TELEFONO]", "[EMAIL]", "[EDAD]");
    }

    @Test
    void matchesNamesIgnoringCaseAndAccents() {
        var candidate = new CandidatePersonalData(8L, "Jorge", "Huamán Torres", "70214589", "+51912345678", "Comas", null);

        var text = anonymizer.anonymize("El equipo de HUAMAN y huamán revisó la data de comas.", candidate).text();

        assertThat(text).isEqualTo("El equipo de [NOMBRE] y [NOMBRE] revisó la data de [DIRECCION].");
    }

    @Test
    void doesNotTouchWordsThatOnlyContainANameToken() {
        var candidate = new CandidatePersonalData(8L, "Ana", "Paz", "70214589", "+51912345678", "Lince", null);

        var text = anonymizer.anonymize("Analizaría la tabla con Ana y la capacidad del equipo.", candidate).text();

        assertThat(text).isEqualTo("Analizaría la tabla con [NOMBRE] y la capacidad del equipo.");
    }

    @Test
    void leavesTechnicalContentIntact() {
        var original = "Usaría un LEFT JOIN y filtraría donde el id del pedido es nulo; el 15 % de caída lo separo por canal.";

        assertThat(anonymizer.anonymize(original, rosa).text()).isEqualTo(original);
    }

    @Test
    void mapsAnonymizedOffsetsBackToTheOriginalText() {
        var original = "Soy Rosa. Separaría la caída en volumen y ticket promedio.";
        var anonymized = anonymizer.anonymize(original, rosa);
        var fragment = "Separaría la caída en volumen y ticket promedio.";
        var start = anonymized.text().indexOf(fragment);
        var end = start + fragment.length();

        var originalStart = anonymized.toOriginalOffset(start, false);
        var originalEnd = anonymized.toOriginalOffset(end, true);

        assertThat(original.substring(originalStart, originalEnd)).isEqualTo(fragment);
    }

    @Test
    void offsetsInsideAMarkerSnapToTheRedactedSpan() {
        var original = "Mi nombre es Rosa y trabajo con SQL.";
        var anonymized = anonymizer.anonymize(original, rosa);
        var markerStart = anonymized.text().indexOf("[NOMBRE]");

        assertThat(anonymized.toOriginalOffset(markerStart + 2, false)).isEqualTo(original.indexOf("Rosa"));
        assertThat(anonymized.toOriginalOffset(markerStart + 2, true)).isEqualTo(original.indexOf("Rosa") + 4);
    }
}
