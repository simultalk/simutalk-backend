package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.AnswerResource;
import pe.upc.simutalk.entities.Answer;

public class AnswerResourceFromEntityAssembler {

    public static AnswerResource toResourceFromEntity(Answer entity) {
        return new AnswerResource(entity.getId(), entity.getQuestionId(), entity.getParentAnswerId(),
                entity.getTranscript(), entity.getAudioUrl(), entity.getDurationSeconds(), entity.getAnsweredAt(),
                entity.isFollowUp());
    }
}
