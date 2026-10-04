package pe.upc.simutalk.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReorderQuestionsResource(
        @Schema(example = "[3, 1, 2]", description = "Todos los ids del guion, en el nuevo orden")
        @NotEmpty List<@NotNull Long> orderedIds) {
}
