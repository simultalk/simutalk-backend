package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CompleteInterviewSessionCommand;
import pe.upc.simutalk.dtos.CreateInterviewSessionCommand;
import pe.upc.simutalk.dtos.RecordAnswerCommand;
import pe.upc.simutalk.dtos.StartInterviewSessionCommand;
import pe.upc.simutalk.entities.Answer;
import pe.upc.simutalk.entities.InterviewSession;

public interface InterviewSessionCommandService {

    InterviewSession handle(CreateInterviewSessionCommand command);

    InterviewSession handle(StartInterviewSessionCommand command);

    Answer handle(RecordAnswerCommand command);

    InterviewSession handle(CompleteInterviewSessionCommand command);
}
