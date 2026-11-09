package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CompleteInterviewSessionCommand;
import pe.upc.simutalk.dtos.CreateInterviewSessionCommand;
import pe.upc.simutalk.dtos.FollowUpQuestion;
import pe.upc.simutalk.dtos.GenerateFollowUpQuestionCommand;
import pe.upc.simutalk.dtos.RecordAnswerCommand;
import pe.upc.simutalk.dtos.StartInterviewSessionCommand;
import pe.upc.simutalk.entities.Answer;
import pe.upc.simutalk.entities.InterviewSession;

public interface InterviewSessionCommandService {

    InterviewSession handle(CreateInterviewSessionCommand command);

    InterviewSession handle(StartInterviewSessionCommand command);

    Answer handle(RecordAnswerCommand command);

    InterviewSession handle(CompleteInterviewSessionCommand command);

    /** US-09: generates (once) the follow-up question of a regular answer and stores it on that answer. */
    FollowUpQuestion handle(GenerateFollowUpQuestionCommand command);
}
