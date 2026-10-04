package pe.upc.simutalk.dtos;

public record AuthenticatedUserResource(Long id, String username, String token) {
}
