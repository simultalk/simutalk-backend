package pe.upc.simutalk.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

public record FollowUpQuestionResource(
        @Schema(example = "1", description = "Respuesta que originó la repregunta: se envía como parentAnswerId al responderla")
        Long parentAnswerId,
        @Schema(example = "5", description = "Pregunta del guion: se envía como questionId al responder la repregunta")
        Long questionId,
        @Schema(description = "Texto de la repregunta generada por la IA")
        String statement) {
}
