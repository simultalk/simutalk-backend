package pe.upc.simutalk.dtos;

import java.math.BigDecimal;
import java.util.List;

/**
 * How the candidate communicated in an interview, computed with fixed rules (no AI) from the answers. It only
 * gives counts and short literal evidence, never a score: it complements the assessment, it does not change it.
 *
 * @param wordsPerMinute             words per minute of response time (from the question until the answer is
 *                                   sent, so it includes thinking time); {@code null} without duration
 * @param fillerWordsPer100Words     filler words per 100 words, one decimal
 * @param answersWithConcreteExample answers with a phrase that introduces a concrete example
 * @param answersWithResult          answers with a phrase that states an achievement or a result
 */
public record CommunicationAnalysis(
        Long interviewSessionId,
        int answerCount,
        int totalWords,
        int totalSeconds,
        Integer wordsPerMinute,
        int fillerWordCount,
        BigDecimal fillerWordsPer100Words,
        List<FillerWordCount> fillerWords,
        int answersWithConcreteExample,
        int answersWithResult,
        List<AnswerCommunication> answers) {

    /** A filler word (or expression) and how many times it was used. */
    public record FillerWordCount(String word, int count) {
    }

    /**
     * @param length                  SHORT (under 25 words), ADEQUATE or LONG (over 250 words)
     * @param concreteExampleEvidence the literal sentence that introduces an example, or {@code null}
     * @param resultEvidence          the literal sentence that states a result, or {@code null}
     */
    public record AnswerCommunication(
            Long answerId,
            Long questionId,
            boolean followUp,
            int words,
            int durationSeconds,
            Integer wordsPerMinute,
            String length,
            int fillerWordCount,
            String concreteExampleEvidence,
            String resultEvidence) {
    }
}
