package pe.upc.simutalk.dtos;

/**
 * The minimum needed to write to a candidate: how to greet them and where. Personal data: it must never be
 * sent to the AI provider.
 *
 * @param email {@code null} when the candidate did not register one
 */
public record CandidateContact(Long candidateId, String firstName, String fullName, String email) {

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }
}
