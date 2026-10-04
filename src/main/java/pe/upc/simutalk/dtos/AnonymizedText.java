package pe.upc.simutalk.dtos;

import java.util.List;

/**
 * A transcript with personal data replaced by markers, plus the mapping needed to translate
 * positions in the anonymized text back to the original one (evidence offsets are always
 * stored against the original transcript).
 */
public record AnonymizedText(String original, String text, List<Replacement> replacements) {

    /**
     * One redacted span: {@code original[originalStart, originalEnd)} became
     * {@code text[anonymizedStart, anonymizedStart + marker.length())}.
     */
    public record Replacement(int originalStart, int originalEnd, int anonymizedStart, String marker) {

        int anonymizedEnd() {
            return anonymizedStart + marker.length();
        }
    }

    /** Maps an anonymized offset to the original text; offsets inside a marker snap to its bounds. */
    public int toOriginalOffset(int anonymizedOffset, boolean isEnd) {
        var shift = 0;
        for (var replacement : replacements) {
            if (anonymizedOffset < replacement.anonymizedStart()) {
                break;
            }
            if (anonymizedOffset < replacement.anonymizedEnd() || (isEnd && anonymizedOffset == replacement.anonymizedEnd())) {
                if (anonymizedOffset == replacement.anonymizedStart() && !isEnd) {
                    return replacement.originalStart();
                }
                return isEnd ? replacement.originalEnd() : replacement.originalStart();
            }
            shift = replacement.originalEnd() - replacement.anonymizedEnd();
        }
        return Math.min(original.length(), Math.max(0, anonymizedOffset + shift));
    }
}
