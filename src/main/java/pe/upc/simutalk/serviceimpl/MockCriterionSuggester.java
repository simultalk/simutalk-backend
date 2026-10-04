package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CriterionSuggestion;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic, network-free suggester for mock mode: ranks a fixed catalogue of competencies by how
 * many of their keywords appear in the job title and description, and returns the top
 * {@value #SUGGESTIONS}. Criteria the posting already has are skipped; if the text matches fewer than
 * {@value #SUGGESTIONS}, competencies that apply to any job complete the list. Never assigns a weight.
 */
final class MockCriterionSuggester {

    static final int SUGGESTIONS = 4;

    /** @param general whether it applies to any job, so it may complete the list without matching the text */
    private record Competency(String name, String description, boolean general, List<String> keywords) {
    }

    /** Order breaks ties and decides which general competencies complete the list first. */
    private static final List<Competency> CATALOGUE = List.of(
            new Competency("Pensamiento analítico", "Descompone problemas y los resuelve apoyándose en datos.", false,
                    List.of("datos", "analisis", "analitic", "sql", "excel", "python", "estadistic", "metric", "reporte", "dashboard")),
            new Competency("Comunicación efectiva", "Explica ideas y hallazgos con claridad a públicos técnicos y no técnicos.", true,
                    List.of("comunica", "presenta", "cliente", "reporte", "explica", "redacci", "stakeholder", "exposici")),
            new Competency("Dominio técnico", "Domina las herramientas y tecnologías propias del puesto.", false,
                    List.of("java", "spring", "angular", "program", "codigo", "desarroll", "api", "cloud", "aws", "base de datos")),
            new Competency("Trabajo en equipo", "Colabora con otras personas y áreas para lograr objetivos comunes.", true,
                    List.of("equipo", "colabor", "multidisciplin", "coordin", "areas")),
            new Competency("Orientación al cliente", "Entiende y atiende las necesidades del cliente interno o externo.", false,
                    List.of("cliente", "servicio", "atencion", "usuario", "satisfaccion")),
            new Competency("Gestión de proyectos", "Planifica, prioriza y da seguimiento a entregables y plazos.", false,
                    List.of("proyecto", "planific", "gestion", "plazo", "scrum", "agil", "priori", "lider")),
            new Competency("Orientación comercial", "Identifica oportunidades de negocio y negocia con criterio.", false,
                    List.of("venta", "comercial", "negoci", "mercado", "cartera")),
            new Competency("Resolución de problemas", "Identifica la causa de un problema y propone soluciones viables.", true,
                    List.of("problema", "resolver", "soluci", "incidencia", "causa", "mejora")),
            new Competency("Adaptabilidad", "Se ajusta a cambios de contexto y aprende con rapidez.", true,
                    List.of("cambio", "aprend", "adapt", "dinamic", "flexib")));

    private MockCriterionSuggester() {
    }

    static List<CriterionSuggestion> suggest(String title, String description, List<String> existingCriteria) {
        var text = fold((title == null ? "" : title) + " " + (description == null ? "" : description));
        var existing = existingCriteria == null ? List.<String>of()
                : existingCriteria.stream().map(MockCriterionSuggester::fold).toList();
        var available = CATALOGUE.stream().filter(competency -> !existing.contains(fold(competency.name()))).toList();
        var candidates = new ArrayList<Competency>(available.stream()
                .filter(competency -> !matches(competency, text).isEmpty())
                .sorted(Comparator.comparingInt((Competency competency) -> matches(competency, text).size()).reversed())
                .toList());
        available.stream()
                .filter(competency -> competency.general() && matches(competency, text).isEmpty())
                .forEach(candidates::add);
        return candidates.stream()
                .limit(SUGGESTIONS)
                .map(competency -> {
                    var matched = matches(competency, text);
                    var rationale = matched.isEmpty()
                            ? "Competencia general recomendada para cualquier puesto"
                            : "La descripción menciona: " + String.join(", ", matched);
                    return new CriterionSuggestion(competency.name(), competency.description(), rationale);
                })
                .toList();
    }

    private static List<String> matches(Competency competency, String foldedText) {
        return competency.keywords().stream().filter(foldedText::contains).toList();
    }

    private static String fold(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
    }
}
