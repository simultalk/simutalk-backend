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
        var words = transcript.split("\\s+").length;
        var score = Math.min(10.0, 4.0 + Math.min(4.0, totalHits * 0.8) + Math.min(2.0, words / 40.0));
        var confidence = Math.min(0.95, 0.55 + Math.min(0.4, hits(excerpt, stems) * 0.1));
        var aiSuspicion = AI_TELLS.stream().anyMatch(transcript.toLowerCase(Locale.ROOT)::contains);
        return new ScoringResult(BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP),
                BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP),
                excerpt, start, start + excerpt.length(), aiSuspicion);
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
