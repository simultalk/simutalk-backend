package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.QuestionSuggestion;
import pe.upc.simutalk.entities.Question;
import pe.upc.simutalk.services.QuestionSuggestionService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Adapter of the {@link QuestionSuggestionService} port. It builds interviews' own prompt, validates
 * the answer against interviews' own schema and uses the shared {@link GenerativeAiClient} only as
 * transport.
 * <ul>
 *   <li>mock mode: {@value MockQuestionSuggester#SUGGESTIONS} questions derived from the criterion's name
 *       and description ({@link MockQuestionSuggester}), no network.</li>
 *   <li>live mode: Gemini. Only {@code statement} and {@code rationale} are read from each item; anything
 *       else the model returns is ignored. Blank or too long statements, repeated ones and questions the
 *       script already has are dropped. If the provider fails, the template questions are returned instead.</li>
 * </ul>
 * Privacy (Ley 29733): the prompt is made only of the job posting's and the criterion's text and the
 * questions already in the script. This class has no access to candidate data at all; keep it that way.
 */
@Slf4j
@Service
public class QuestionSuggestionServiceImpl implements QuestionSuggestionService {

    private final GenerativeAiClient aiClient;
    private final ObjectMapper objectMapper;

    public QuestionSuggestionServiceImpl(GenerativeAiClient aiClient, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<QuestionSuggestion> suggest(String jobTitle, String jobDescription, String criterionName,
                                            String criterionDescription, List<String> existingQuestions) {
        var existing = existingQuestions == null ? List.<String>of() : existingQuestions;
        if (!aiClient.isLive()) {
            return MockQuestionSuggester.suggest(criterionName, criterionDescription, existing);
        }
        var parsed = aiClient.generate(prompt(jobTitle, jobDescription, criterionName, criterionDescription, existing))
                .map(text -> parse(text, existing))
                .orElse(List.of());
        if (parsed.isEmpty()) {
            log.warn("AI question suggestions unavailable; answering with template-based suggestions");
            return MockQuestionSuggester.suggest(criterionName, criterionDescription, existing);
        }
        return parsed;
    }

    /** Keeps only well-formed, new, distinct questions; nothing but the statement and the rationale is read. */
    List<QuestionSuggestion> parse(String text, List<String> existingQuestions) {
        try {
            var root = objectMapper.readTree(text);
            var items = root.isArray() ? root : root.path("questions");
            var taken = new LinkedHashSet<String>();
            existingQuestions.forEach(statement -> taken.add(MockQuestionSuggester.fold(statement)));
            var suggestions = new ArrayList<QuestionSuggestion>();
            for (var item : items) {
                var statement = item.path("statement").asString("").strip();
                if (statement.isEmpty() || statement.length() > Question.STATEMENT_MAX_LENGTH
                        || !taken.add(MockQuestionSuggester.fold(statement))) {
                    continue;
                }
                suggestions.add(new QuestionSuggestion(statement, item.path("rationale").asString("")));
                if (suggestions.size() == MockQuestionSuggester.SUGGESTIONS) {
                    break;
                }
            }
            return suggestions;
        } catch (Exception ex) {
            log.warn("Could not read the AI question suggestions: {}", ex.toString());
            return List.of();
        }
    }

    static String prompt(String jobTitle, String jobDescription, String criterionName, String criterionDescription,
                         List<String> existingQuestions) {
        return """
                Eres un especialista en selección de personal. Propón hasta %d preguntas para una entrevista
                asincrónica de preselección que permitan evaluar ÚNICAMENTE el CRITERIO en quienes postulan al PUESTO.
                Cada pregunta debe ser abierta, clara y poder responderse hablando en pocos minutos.
                No pidas datos personales del postulante.

                PUESTO: %s
                DESCRIPCIÓN DEL PUESTO:
                <<<
                %s
                >>>
                CRITERIO: %s
                DESCRIPCIÓN DEL CRITERIO: %s
                PREGUNTAS QUE YA EXISTEN EN EL GUION (no las repitas): %s

                Devuelve solo un arreglo JSON de objetos con estas claves:
                - "statement": la pregunta, dirigida al postulante en segunda persona (máximo %d caracteres).
                - "rationale": qué aspecto del criterio permite observar la respuesta.
                """.formatted(MockQuestionSuggester.SUGGESTIONS, jobTitle == null ? "" : jobTitle,
                jobDescription == null ? "" : jobDescription, criterionName == null ? "" : criterionName,
                criterionDescription == null ? "" : criterionDescription,
                existingQuestions.isEmpty() ? "ninguna" : String.join(" | ", existingQuestions),
                Question.STATEMENT_MAX_LENGTH);
    }
}
