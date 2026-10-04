package pe.upc.simutalk.dtos;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stable JSON shape for paginated responses (Spring's PageImpl is not meant to be serialized).
 *
 * @param page zero-based page index
 */
public record PageResource<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResource<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResource<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
