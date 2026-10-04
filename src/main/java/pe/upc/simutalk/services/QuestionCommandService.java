package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CreateQuestionCommand;
import pe.upc.simutalk.dtos.DeleteQuestionCommand;
import pe.upc.simutalk.dtos.ReorderQuestionsCommand;
import pe.upc.simutalk.dtos.UpdateQuestionCommand;
import pe.upc.simutalk.entities.Question;

import java.util.List;

public interface QuestionCommandService {

    Question handle(CreateQuestionCommand command);

    Question handle(UpdateQuestionCommand command);

    void handle(DeleteQuestionCommand command);

    /** @return the script in its new order */
    List<Question> handle(ReorderQuestionsCommand command);
}
