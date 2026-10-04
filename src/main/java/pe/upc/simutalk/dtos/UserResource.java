package pe.upc.simutalk.dtos;

import java.util.List;

public record UserResource(Long id, String username, List<String> roles) {
}
