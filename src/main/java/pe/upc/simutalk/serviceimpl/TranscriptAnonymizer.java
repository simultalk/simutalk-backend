package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.AnonymizedText;
import pe.upc.simutalk.dtos.CandidatePersonalData;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Redacts the candidate's personal data from a transcript before it leaves the platform
 * (Ley 29733 and CLAUDE.md "Anonimización"): the candidate's own name, surnames, document,
 * phone and district, plus any e-mail, URL, phone number, identity-document-like number, age
 * or date found in the text. Each span becomes a marker such as {@code [NOMBRE]}.
 */
@Component
public class TranscriptAnonymizer {

    public static final String NAME = "[NOMBRE]";
    public static final String EMAIL = "[EMAIL]";
    public static final String PHONE = "[TELEFONO]";
    public static final String DOCUMENT = "[DOCUMENTO]";
    public static final String ADDRESS = "[DIRECCION]";
    public static final String URL = "[URL]";
    public static final String AGE = "[EDAD]";
    public static final String DATE = "[FECHA]";

    private static final int MIN_NAME_TOKEN_LENGTH = 3;

    private static final List<Rule> GENERIC_RULES = List.of(
            new Rule(Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"), EMAIL),
            new Rule(Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+"), URL),
            new Rule(Pattern.compile("(?<!\\d)(?:\\+?51[\\s-]?)?9\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{3}(?!\\d)"), PHONE),
            new Rule(Pattern.compile("(?<!\\d)\\d{8,12}(?!\\d)"), DOCUMENT),
            new Rule(Pattern.compile("(?i)(?<!\\d)\\d{1,3}\\s+años(?:\\s+de\\s+edad)?"), AGE),
            new Rule(Pattern.compile("(?<!\\d)\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}(?!\\d)"), DATE));

    public AnonymizedText anonymize(String transcript, CandidatePersonalData candidate) {
        if (transcript == null) {
            throw new IllegalArgumentException("Transcript is required");
        }
        var matches = new ArrayList<Match>();
        if (candidate != null) {
            addLiteral(matches, transcript, candidate.documentNumber(), DOCUMENT);
            addLiteral(matches, transcript, candidate.phone(), PHONE);
            addLiteral(matches, transcript, candidate.email(), EMAIL);
            addLiteral(matches, transcript, candidate.district(), ADDRESS);
            nameTokens(candidate).forEach(token -> addLiteral(matches, transcript, token, NAME));
        }
        GENERIC_RULES.forEach(rule -> rule.pattern().matcher(transcript).results()
                .forEach(result -> matches.add(new Match(result.start(), result.end(), rule.marker()))));
        return build(transcript, withoutOverlaps(matches));
    }

    private static List<String> nameTokens(CandidatePersonalData candidate) {
        var tokens = new ArrayList<String>();
        for (var part : List.of(nullToEmpty(candidate.firstName()), nullToEmpty(candidate.lastName()))) {
            if (!part.isBlank()) {
                tokens.add(part.strip());
                for (var token : part.strip().split("\\s+")) {
                    if (token.length() >= MIN_NAME_TOKEN_LENGTH) {
                        tokens.add(token);
                    }
                }
            }
        }
        return tokens;
    }

    /** Case- and accent-insensitive whole-word search of {@code literal}. */
    private static void addLiteral(List<Match> matches, String text, String literal, String marker) {
        if (literal == null || literal.isBlank()) {
            return;
        }
        var foldedText = fold(text);
        var foldedLiteral = fold(literal.strip());
        var pattern = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(foldedLiteral) + "(?![\\p{L}\\p{N}])");
        pattern.matcher(foldedText).results()
                .forEach(result -> matches.add(new Match(result.start(), result.end(), marker)));
    }

    /**
     * Lower case without accents. Removing combining marks keeps one char per char for the
     * Latin letters used in Spanish, so offsets in the folded text match the original.
     */
    private static String fold(String value) {
        var decomposed = Normalizer.normalize(value.toLowerCase(), Normalizer.Form.NFD);
        var folded = decomposed.replaceAll("\\p{M}", "");
        return folded.length() == value.length() ? folded : value.toLowerCase();
    }

    /** Keeps the earliest match; on ties, the longest. Overlapping later matches are dropped. */
    private static List<Match> withoutOverlaps(List<Match> matches) {
        var sorted = matches.stream()
                .sorted(Comparator.comparingInt(Match::start).thenComparing(Comparator.comparingInt(Match::end).reversed()))
                .toList();
        var kept = new ArrayList<Match>();
        var lastEnd = -1;
        for (var match : sorted) {
            if (match.start() >= lastEnd) {
                kept.add(match);
                lastEnd = match.end();
            }
        }
        return kept;
    }

    private static AnonymizedText build(String original, List<Match> matches) {
        var text = new StringBuilder();
        var replacements = new ArrayList<AnonymizedText.Replacement>();
        var cursor = 0;
        for (var match : matches) {
            text.append(original, cursor, match.start());
            replacements.add(new AnonymizedText.Replacement(match.start(), match.end(), text.length(), match.marker()));
            text.append(match.marker());
            cursor = match.end();
        }
        text.append(original.substring(cursor));
        return new AnonymizedText(original, text.toString(), replacements);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Rule(Pattern pattern, String marker) {
    }

    private record Match(int start, int end, String marker) {
    }
}
