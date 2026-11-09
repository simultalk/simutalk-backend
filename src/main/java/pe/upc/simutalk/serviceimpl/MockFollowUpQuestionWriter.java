package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.entities.Question;

/**
 * Deterministic, network-free follow-up question for mock mode and for when the AI provider fails. It is
 * always in Spanish: a short answer is asked for a concrete example, a longer one for a reflection on the
 * result. The criterion's name is shortened if needed so the question never exceeds the limit of a script
 * question.
 */
final class MockFollowUpQuestionWriter {

    /** Answers with fewer words than this are considered short. */
    static final int SHORT_ANSWER_WORDS = 25;

    private static final String NAME_SLOT = "{name}";

    private static final String SHORT_ANSWER_TEMPLATE = "Tu respuesta fue breve. Para conocer mejor cómo aplicas «{name}», "
            + "¿podrías contar una situación real concreta, qué hiciste tú personalmente y cuál fue el resultado?";

    private static final String LONG_ANSWER_TEMPLATE = "Gracias por tu respuesta. Pensando en esa misma situación y en «{name}», "
            + "¿qué harías distinto si la vivieras otra vez y cómo comprobarías que el resultado fue bueno?";

    private MockFollowUpQuestionWriter() {
    }

    static String write(String criterionName, String anonymizedAnswer) {
        var name = criterionName == null || criterionName.isBlank() ? "esta competencia" : criterionName.strip();
        var template = wordCount(anonymizedAnswer) < SHORT_ANSWER_WORDS ? SHORT_ANSWER_TEMPLATE : LONG_ANSWER_TEMPLATE;
        var room = Question.STATEMENT_MAX_LENGTH - (template.length() - NAME_SLOT.length());
        return template.replace(NAME_SLOT, shorten(name, room));
    }

    private static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.strip().split("\\s+").length;
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
