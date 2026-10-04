package pe.upc.simutalk.dtos;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * Stable pseudonym such as {@code CANDIDATO-A-4471} used instead of the candidate's name and
 * document in anonymized rankings. Derived with HMAC-SHA256 from the candidate and the job
 * posting, so it is stable for a posting, differs between postings, and cannot be reversed by
 * enumerating ids without the secret.
 */
public record CandidateCode(String value) {

    private static final String PREFIX = "CANDIDATO";

    public CandidateCode {
        if (value == null || !value.matches(PREFIX + "-[A-Z]-\\d{4}")) {
            throw new IllegalArgumentException("Invalid candidate code: " + value);
        }
    }

    public static CandidateCode of(Long candidateId, Long jobPostingId, String secret) {
        if (candidateId == null || jobPostingId == null || secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Candidate, job posting and secret are required");
        }
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var digest = mac.doFinal((candidateId + ":" + jobPostingId).getBytes(StandardCharsets.UTF_8));
            var letter = (char) ('A' + Math.floorMod(digest[0], 26));
            var number = 1000 + Math.floorMod(ByteBuffer.wrap(digest, 1, 4).getInt(), 9000);
            return new CandidateCode("%s-%c-%04d".formatted(PREFIX, letter, number));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HmacSHA256 is not available", ex);
        }
    }
}
