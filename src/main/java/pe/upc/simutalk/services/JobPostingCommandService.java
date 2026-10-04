package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.AddEvaluationCriterionCommand;
import pe.upc.simutalk.dtos.ChangeJobPostingStatusCommand;
import pe.upc.simutalk.dtos.CreateJobPostingCommand;
import pe.upc.simutalk.dtos.DeleteJobPostingCommand;
import pe.upc.simutalk.dtos.RemoveEvaluationCriterionCommand;
import pe.upc.simutalk.dtos.UpdateEvaluationCriterionCommand;
import pe.upc.simutalk.dtos.UpdateJobPostingCommand;
import pe.upc.simutalk.entities.EvaluationCriterion;
import pe.upc.simutalk.entities.JobPosting;

public interface JobPostingCommandService {

    JobPosting handle(CreateJobPostingCommand command);

    JobPosting handle(UpdateJobPostingCommand command);

    void handle(DeleteJobPostingCommand command);

    JobPosting handle(ChangeJobPostingStatusCommand command);

    EvaluationCriterion handle(AddEvaluationCriterionCommand command);

    EvaluationCriterion handle(UpdateEvaluationCriterionCommand command);

    void handle(RemoveEvaluationCriterionCommand command);
}
