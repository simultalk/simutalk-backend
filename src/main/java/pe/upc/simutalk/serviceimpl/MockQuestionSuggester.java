package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.QuestionSuggestion;
import pe.upc.simutalk.entities.Question;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic, network-free suggester for mock mode: fills a fixed list of question templates with
 * the criterion's name and description and returns the first {@value #SUGGESTIONS} that are not in the
 * script yet. Statements never exceed the limit of a script question: a long description is shortened.
 */
final class MockQuestionSuggester {

    static final int SUGGESTIONS = 3;

    private static final String DESCRIPTION_SLOT = "{description}";
    private static final String NAME_SLOT = "{name}";

    /** @param text uses {@code {name}} and, optionally, {@code {description}} */
    private record Template(String text, String rationale) {

        boolean needsDescription() {
            return text.contains(DESCRIPTION_SLOT);
        }
    }

    /** Order decides which questions are proposed first. */
    private static final List<Template> TEMPLATES = List.of(
            new Template("Cuéntame una situación concreta en la que hayas puesto en práctica la competencia «{name}». "
                    + "¿Qué hiciste y cuál fue el resultado?",
                    "Pregunta conductual: una experiencia real muestra cómo aplicó la competencia"),
            new Template("Para este puesto, «{name}» significa: «{description}». "
                    + "Describe paso a paso cómo lo harías ante un caso real.",
                    "Pregunta situacional basada en la descripción del criterio"),
            new Template("¿Cuál ha sido tu mayor dificultad relacionada con «{name}» y qué hiciste para superarla?",
                    "Explora autocrítica y capacidad de aprendizaje sobre la competencia"),
            new Template("Imagina que en tu primer mes debes demostrar «{name}» con poco tiempo y poca información. "
                    + "¿Qué harías primero y por qué?",
                    "Pregunta hipotética: muestra cómo prioriza al aplicar la competencia"),
            new Template("¿Cómo compruebas que tu trabajo cumple con lo siguiente: «{description}»? Da un ejemplo.",
                    "Explora los criterios de calidad que aplica, a partir de la descripción del criterio"),
            new Template("Si tuvieras que explicarle «{name}» a una persona nueva en el equipo, "
                    + "¿qué le dirías y con qué ejemplo?",
                    "Explicar la competencia a otra persona revela cuánto la domina"));

    private MockQuestionSuggester() {
    }

    static List<QuestionSuggestion> suggest(String criterionName, String criterionDescription,
                                            List<String> existingQuestions) {
        var name = criterionName == null ? "" : criterionName.strip();
        if (name.isEmpty()) {
            return List.of();
        }
        var description = criterionDescription == null ? "" : criterionDescription.strip().replaceAll("[.\\s]+$", "");
        var taken = new ArrayList<String>();
        if (existingQuestions != null) {
            existingQuestions.forEach(statement -> taken.add(fold(statement)));
        }
        var suggestions = new ArrayList<QuestionSuggestion>();
        for (var template : TEMPLATES) {
            if (template.needsDescription() && description.isEmpty()) {
                continue;
            }
            var statement = fill(template, name, description);
            if (taken.contains(fold(statement))) {
                continue;
            }
            suggestions.add(new QuestionSuggestion(statement, template.rationale()));
            if (suggestions.size() == SUGGESTIONS) {
                break;
            }
        }
        return suggestions;
    }

    /** Lower case, without accents and with single spaces: how two statements are compared. */
    static String fold(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private static String fill(Template template, String name, String description) {
        var withName = template.text().replace(NAME_SLOT, name);
        if (!template.needsDescription()) {
            return withName;
        }
        var room = Question.STATEMENT_MAX_LENGTH - (withName.length() - DESCRIPTION_SLOT.length());
        return withName.replace(DESCRIPTION_SLOT, shorten(description, room));
    }

    /** Cuts at a word boundary and marks the cut with an ellipsis. */
    private static String shorten(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        var cut = text.substring(0, Math.max(0, maxLength - 1));
        var lastSpace = cut.lastIndexOf(' ');
        return (lastSpace > 0 ? cut.substring(0, lastSpace) : cut).strip() + "…";
    }
}
