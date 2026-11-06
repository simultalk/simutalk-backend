package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.services.AnswerScoringService.ScoringResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deterministic, network-free scorer for {@code external.ai.mode=mock}. It picks the sentence of
 * the answer that shares the most word stems with the criterion as the evidence and scores by
 * that overlap and the answer's length. Good enough for demos and tests; not a real evaluator.
 */
final class MockAnswerScorer {

    private static final Pattern SENTENCE = Pattern.compile("[^.!?]+[.!?]?");
    private static final int STEM_LENGTH = 5;
    private static final Set<String> AI_TELLS = Set.of("como modelo de lenguaje", "como asistente de ia", "como ia,");
    /** Evasive answers, matched on the lower-cased text without accents. */
    private static final Pattern EVASIVE = Pattern.compile("\\b(no se|no lo se|ni idea|no tengo idea|no sabria|no recuerdo"
            + "|no estoy seguro|no entiendo la pregunta|i don.?t know|no idea|not sure)\\b");
    /** Up to this many words, an evasive answer with almost no reference to the criterion scores low. */
    static final int EVASIVE_MAX_WORDS = 25;
    /** Fewer words than this, with no reference to the criterion, is an empty answer. */
    static final int EMPTY_MAX_WORDS = 6;
    static final double EVASIVE_SCORE = 1.0;
    static final double EMPTY_SCORE = 1.5;

    private MockAnswerScorer() {
    }

    static ScoringResult score(String transcript, String criterionName, String criterionDescription) {
        if (transcript == null || transcript.isBlank()) {
            return ScoringResult.unavailable();
        }
        var stems = stems(criterionName + " " + (criterionDescription == null ? "" : criterionDescription));
        var best = SENTENCE.matcher(transcript).results()
                .filter(match -> !match.group().isBlank())
                .max((a, b) -> Integer.compare(hits(a.group(), stems), hits(b.group(), stems)) != 0
                        ? Integer.compare(hits(a.group(), stems), hits(b.group(), stems))
                        : Integer.compare(b.start(), a.start()))
                .orElse(null);
        if (best == null) {
            return ScoringResult.unavailable();
        }
        var raw = best.group();
        var leading = raw.length() - raw.stripLeading().length();
        var excerpt = raw.strip();
        var start = best.start() + leading;
        var totalHits = hits(transcript, stems);
        var words = transcript.strip().split("\\s+").length;
        var evasive = words <= EVASIVE_MAX_WORDS && totalHits <= 1 && EVASIVE.matcher(fold(transcript)).find();
        var empty = words < EMPTY_MAX_WORDS && totalHits == 0;
        var score = evasive ? EVASIVE_SCORE : empty ? EMPTY_SCORE
                : Math.min(10.0, 4.0 + Math.min(4.0, totalHits * 0.8) + Math.min(2.0, words / 40.0));
        var confidence = evasive || empty ? 0.8 : Math.min(0.95, 0.55 + Math.min(0.4, hits(excerpt, stems) * 0.1));
        var rationale = evasive ? "Respuesta evasiva: no aporta evidencia sobre el criterio."
                : empty ? "Respuesta demasiado breve y sin relación con el criterio." : null;
        var aiSuspicion = AI_TELLS.stream().anyMatch(transcript.toLowerCase(Locale.ROOT)::contains);
        return new ScoringResult(BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP),
                BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP),
                excerpt, start, start + excerpt.length(), aiSuspicion, rationale);
    }

    private static Set<String> stems(String text) {
        return Arrays.stream(fold(text).split("[^\\p{L}]+"))
                .filter(word -> word.length() >= STEM_LENGTH)
                .map(word -> word.substring(0, STEM_LENGTH))
                .collect(Collectors.toSet());
    }

    private static int hits(String text, Set<String> stems) {
        return (int) Arrays.stream(fold(text).split("[^\\p{L}]+"))
                .filter(word -> word.length() >= STEM_LENGTH && stems.contains(word.substring(0, STEM_LENGTH)))
                .count();
    }

    private static String fold(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
