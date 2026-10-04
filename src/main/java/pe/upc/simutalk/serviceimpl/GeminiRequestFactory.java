package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.services.AnswerScoringService.CriterionFeedback;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds assessment's scoring prompt and, for inspection in tests, the full {@code generateContent}
 * body the shared {@link GenerativeAiClient} sends with it.
 * <p>
 * Privacy (Ley 29733): the payload is made ONLY of the anonymized transcript and the criterion.
 * This class has no access to candidate data at all; keep it that way.
 */
public final class GeminiRequestFactory {

    private GeminiRequestFactory() {
    }

    public static Map<String, Object> build(String anonymizedTranscript, String criterionName, String criterionDescription) {
        return GenerativeAiClient.requestBody(prompt(anonymizedTranscript, criterionName, criterionDescription));
    }

    static String prompt(String anonymizedTranscript, String criterionName, String criterionDescription) {
        return """
                Eres un evaluador de entrevistas de preselección. Evalúa la RESPUESTA únicamente contra el CRITERIO.
                Los datos personales ya fueron reemplazados por marcadores como [NOMBRE]; no intentes inferirlos.

                CRITERIO: %s
                DESCRIPCIÓN DEL CRITERIO: %s

                RESPUESTA:
                <<<
                %s
                >>>

                Devuelve solo un objeto JSON con estas claves:
                - "score": número de 0 a 10 con un decimal.
                - "confidence": número de 0 a 1 que indique qué tan seguro estás.
                - "excerpt": el fragmento de la RESPUESTA, copiado LITERALMENTE, que mejor sustenta el puntaje.
                - "aiGeneratedSuspicion": true si la respuesta parece redactada por un asistente de IA y no por la persona.
                """.formatted(criterionName, criterionDescription == null ? "" : criterionDescription, anonymizedTranscript);
    }

    /**
     * Feedback prompt: criterion names, scores, weights and ANONYMIZED excerpts only. No ranking, no other
     * candidates, no integrity flags, no candidate data.
     */
    static String feedbackPrompt(String weightedScore, List<CriterionFeedback> criteria) {
        var lines = criteria.stream()
                .map(criterion -> "- %s (%s): puntaje %s de 10, peso %d %%%s".formatted(criterion.criterionName(),
                        criterion.criterionKind(), criterion.score().toPlainString(), criterion.weightApplied(),
                        criterion.anonymizedExcerpt() == null ? "" : "; fragmento de su respuesta: \"%s\"".formatted(
                                criterion.anonymizedExcerpt())))
                .collect(Collectors.joining("\n"));
        return """
                Eres un reclutador que da retroalimentación breve y respetuosa a un postulante sobre su entrevista.
                Los datos personales ya fueron reemplazados por marcadores como [NOMBRE]; no intentes inferirlos.
                Explica qué sostuvo su puntaje y en qué criterio perdió más puntos, y cómo podría mejorar.
                No menciones posiciones, rankings, otros postulantes ni sospechas de ningún tipo.

                PUNTAJE PONDERADO: %s de 10
                CRITERIOS:
                %s

                Devuelve solo un objeto JSON con la clave "feedback": un texto en español de máximo 800 caracteres,
                dirigido al postulante en segunda persona.
                """.formatted(weightedScore, lines);
    }
}
