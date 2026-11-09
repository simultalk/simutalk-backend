package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CommunicationAnalysis;
import pe.upc.simutalk.dtos.CommunicationAnalysis.AnswerCommunication;
import pe.upc.simutalk.dtos.CommunicationAnalysis.FillerWordCount;
import pe.upc.simutalk.entities.Answer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Pure rules of the communication analysis of an interview (Spanish and English). Deterministic and explainable:
 * every flag points to the literal sentence that triggered it, and the patterns avoid common false positives
 * (for instance, "este" counts as a filler only when followed by a pause: "este, …" but not "este proyecto").
 * Matching runs on the lower-cased text without accents.
 */
public final class CommunicationAnalyzer {

    public static final int SHORT_ANSWER_WORDS = 25;
    public static final int LONG_ANSWER_WORDS = 250;
    static final int EVIDENCE_MAX_LENGTH = 200;

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+(?:['’-][\\p{L}\\p{N}]+)*");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?…])\\s+|\\n+");

    /** Label shown to the user -> pattern. The order is the order of the report on ties. */
    private static final Map<String, Pattern> FILLERS = new LinkedHashMap<>();

    static {
        FILLERS.put("eh", Pattern.compile("\\beh+\\b"));
        FILLERS.put("mmm", Pattern.compile("\\b(?:em+|m{2,}|hmm+)\\b"));
        FILLERS.put("este…", Pattern.compile("\\beste\\s*(?:,|\\.{2,}|…)"));
        FILLERS.put("o sea", Pattern.compile("\\bo\\s?sea\\b"));
        FILLERS.put("digamos", Pattern.compile("\\bdigamos\\b"));
        FILLERS.put("bueno,", Pattern.compile("\\bbueno\\s*,"));
        FILLERS.put("tipo,", Pattern.compile("\\btipo\\s*,"));
        FILLERS.put("como que", Pattern.compile("\\bcomo que\\b"));
        FILLERS.put("um", Pattern.compile("\\bu+m+\\b"));
        FILLERS.put("uh", Pattern.compile("\\bu+h+\\b"));
        FILLERS.put("like,", Pattern.compile("\\blike\\s*,"));
        FILLERS.put("you know", Pattern.compile("\\byou know\\b"));
        FILLERS.put("i mean", Pattern.compile("\\bi mean\\b"));
    }

    private static final Pattern CONCRETE_EXAMPLE = Pattern.compile("\\b(?:por ejemplo|una vez|en el proyecto"
            + "|en mi (?:anterior|ultimo|ultima|trabajo|empresa|equipo|practica|practicas|proyecto)"
            + "|cuando (?:trabaje|estaba|estuve|lidere|tuve|hice)"
            + "|for example|for instance|in my (?:last|previous|current) (?:job|role|team|project|company)|once i)\\b");

    private static final Pattern RESULT = Pattern.compile("\\b(?:logre|logramos|consegui|conseguimos|reduje|redujimos"
            + "|aumente|aumentamos|incremente|incrementamos|mejore|mejoramos|ahorre|ahorramos|el resultado|como resultado"
            + "|achieved|reduced|increased|improved|saved|as a result)\\b|\\d+(?:[.,]\\d+)?\\s?%");

    private CommunicationAnalyzer() {
    }

    public static CommunicationAnalysis analyze(Long interviewSessionId, List<Answer> answers) {
        var perAnswer = new ArrayList<AnswerCommunication>();
        var fillerTotals = new LinkedHashMap<String, Integer>();
        var totalWords = 0;
        var totalSeconds = 0;
        var withExample = 0;
        var withResult = 0;
        for (var answer : answers == null ? List.<Answer>of() : answers) {
            var text = answer.getTranscript() == null ? "" : answer.getTranscript();
            var folded = fold(text);
            var words = countWords(text);
            var seconds = Math.max(0, answer.getDurationSeconds());
            var fillers = 0;
            for (var filler : FILLERS.entrySet()) {
                var count = (int) filler.getValue().matcher(folded).results().count();
                if (count > 0) {
                    fillers += count;
                    fillerTotals.merge(filler.getKey(), count, Integer::sum);
                }
            }
            var example = firstSentenceMatching(text, CONCRETE_EXAMPLE);
            var result = firstSentenceMatching(text, RESULT);
            withExample += example == null ? 0 : 1;
            withResult += result == null ? 0 : 1;
            totalWords += words;
            totalSeconds += seconds;
            perAnswer.add(new AnswerCommunication(answer.getId(), answer.getQuestionId(), answer.isFollowUp(), words,
                    seconds, wordsPerMinute(words, seconds), length(words), fillers, example, result));
        }
        var fillerCount = fillerTotals.values().stream().mapToInt(Integer::intValue).sum();
        var fillerWords = fillerTotals.entrySet().stream()
                .map(entry -> new FillerWordCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(FillerWordCount::count).reversed())
                .toList();
        var per100 = totalWords == 0 ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(fillerCount * 100.0 / totalWords).setScale(1, RoundingMode.HALF_UP);
        return new CommunicationAnalysis(interviewSessionId, perAnswer.size(), totalWords, totalSeconds,
                wordsPerMinute(totalWords, totalSeconds), fillerCount, per100, fillerWords, withExample, withResult,
                List.copyOf(perAnswer));
    }

    static int countWords(String text) {
        return text == null ? 0 : (int) WORD.matcher(text).results().count();
    }

    static String length(int words) {
        return words < SHORT_ANSWER_WORDS ? "SHORT" : words > LONG_ANSWER_WORDS ? "LONG" : "ADEQUATE";
    }

    private static Integer wordsPerMinute(int words, int seconds) {
        return seconds <= 0 ? null : (int) Math.round(words * 60.0 / seconds);
    }

    /** The first sentence of {@code text} (as written) whose folded form matches {@code pattern}. */
    private static String firstSentenceMatching(String text, Pattern pattern) {
        for (var sentence : SENTENCE_END.split(text)) {
            var trimmed = sentence.strip();
            if (!trimmed.isEmpty() && pattern.matcher(fold(trimmed)).find()) {
                return trimmed.length() <= EVIDENCE_MAX_LENGTH ? trimmed
                        : trimmed.substring(0, EVIDENCE_MAX_LENGTH - 1).strip() + "…";
            }
        }
        return null;
    }

    private static String fold(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
