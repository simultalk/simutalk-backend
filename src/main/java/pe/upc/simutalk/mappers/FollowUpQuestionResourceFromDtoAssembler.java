package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.FollowUpQuestion;
import pe.upc.simutalk.dtos.FollowUpQuestionResource;

public class FollowUpQuestionResourceFromDtoAssembler {

    public static FollowUpQuestionResource toResourceFromDto(FollowUpQuestion followUpQuestion) {
        return new FollowUpQuestionResource(followUpQuestion.parentAnswerId(), followUpQuestion.questionId(),
                followUpQuestion.statement());
    }
}
