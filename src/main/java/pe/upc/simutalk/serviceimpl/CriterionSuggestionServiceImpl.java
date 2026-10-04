package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CriterionSuggestion;
import pe.upc.simutalk.entities.EvaluationCriterion;
import pe.upc.simutalk.services.CriterionSuggestionService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Adapter of the {@link CriterionSuggestionService} port. It builds recruitment's own prompt, validates
 * the answer against recruitment's own schema and uses the shared {@link GenerativeAiClient} only as
 * transport.
 * <ul>
 *   <li>mock mode: {@value MockCriterionSuggester#SUGGESTIONS} criteria derived from keywords of the
 *       description ({@link MockCriterionSuggester}), no network.</li>
 *   <li>live mode: Gemini. Only {@code name}, {@code description} and {@code rationale} are read from each
 *       item; anything else the model returns (a weight, for instance) is ignored. Invalid items, repeated
 *       names and criteria the posting already has are dropped. If the provider fails, the keyword
 *       suggestions are returned instead.</li>
 * </ul>
 */
@Slf4j
@Service
public class CriterionSuggestionServiceImpl implements CriterionSuggestionService {

    private final GenerativeAiClient aiClient;
    private final ObjectMapper objectMapper;

    public CriterionSuggestionServiceImpl(GenerativeAiClient aiClient, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CriterionSuggestion> suggest(String title, String description, List<String> existingCriteria) {
        var existing = existingCriteria == null ? List.<String>of() : existingCriteria;
        if (!aiClient.isLive()) {
            return MockCriterionSuggester.suggest(title, description, existing);
        }
        var parsed = aiClient.generate(prompt(title, description, existing))
                .map(text -> parse(text, existing))
                .orElse(List.of());
        if (parsed.isEmpty()) {
            log.warn("AI criterion suggestions unavailable; answering with keyword-based suggestions");
            return MockCriterionSuggester.suggest(title, description, existing);
        }
        return parsed;
    }

    /** Keeps only well-formed, new, distinct suggestions; never reads a weight. */
    List<CriterionSuggestion> parse(String text, List<String> existingCriteria) {
        try {
            var root = objectMapper.readTree(text);
            var items = root.isArray() ? root : root.path("criteria");
            var taken = new LinkedHashSet<String>();
            existingCriteria.forEach(name -> taken.add(name.strip().toLowerCase(Locale.ROOT)));
            var suggestions = new ArrayList<CriterionSuggestion>();
            for (var item : items) {
                var name = item.path("name").asString("").strip();
                var description = item.path("description").asString("").strip();
                if (name.isEmpty() || description.isEmpty() || name.length() > EvaluationCriterion.NAME_MAX_LENGTH
                        || description.length() > EvaluationCriterion.DESCRIPTION_MAX_LENGTH
                        || !taken.add(name.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                suggestions.add(new CriterionSuggestion(name, description, item.path("rationale").asString("")));
                if (suggestions.size() == MockCriterionSuggester.SUGGESTIONS) {
                    break;
                }
            }
            return suggestions;
        } catch (Exception ex) {
            log.warn("Could not read the AI criterion suggestions: {}", ex.toString());
            return List.of();
        }
    }

    static String prompt(String title, String description, List<String> existingCriteria) {
        return """
                Eres un especialista en selección de personal. A partir del PUESTO, propone hasta %d criterios de
                evaluación (competencias) para una entrevista de preselección. No asignes pesos ni porcentajes:
                el peso de cada criterio lo decide siempre el reclutador.

                PUESTO: %s
                DESCRIPCIÓN:
                <<<
                %s
                >>>
                CRITERIOS QUE YA EXISTEN (no los repitas): %s

                Devuelve solo un arreglo JSON de objetos con estas claves:
                - "name": nombre corto del criterio (máximo %d caracteres).
                - "description": qué evalúa el criterio, en una oración.
                - "rationale": qué parte de la descripción lo justifica.
                """.formatted(MockCriterionSuggester.SUGGESTIONS, title == null ? "" : title,
                description == null ? "" : description,
                existingCriteria.isEmpty() ? "ninguno" : String.join(", ", existingCriteria),
                EvaluationCriterion.NAME_MAX_LENGTH);
    }
}
