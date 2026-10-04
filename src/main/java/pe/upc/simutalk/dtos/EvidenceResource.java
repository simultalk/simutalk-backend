package pe.upc.simutalk.dtos;

/**
 * @param startOffset position of the excerpt in the original transcript (inclusive)
 * @param endOffset   position of the excerpt in the original transcript (exclusive)
 */
public record EvidenceResource(Long id, Long answerId, String excerpt, int startOffset, int endOffset) {
}
