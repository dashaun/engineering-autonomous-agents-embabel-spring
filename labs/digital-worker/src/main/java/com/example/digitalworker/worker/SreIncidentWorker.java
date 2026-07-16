package com.example.digitalworker.worker;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.core.ActionRetryPolicy;
import com.example.digitalworker.domain.DockerComposeStatus;
import com.example.digitalworker.domain.IncidentRequest;
import com.example.digitalworker.domain.IncidentResponseReport;
import com.example.digitalworker.domain.IncidentWorkflowReport;
import com.example.digitalworker.domain.RunbookAssessment;
import com.example.digitalworker.domain.ServiceObservation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@Agent(
        name = "sre-incident-worker",
        description = "Produce a bounded incident response for human approval",
        actionRetryPolicy = ActionRetryPolicy.FIRE_ONCE)
public class SreIncidentWorker {

    private final ComposeStatusReader statusReader;
    private final IncidentRunbook runbook;
    private final IncidentAnalyst analyst;

    public SreIncidentWorker(
            ComposeStatusReader statusReader,
            IncidentRunbook runbook,
            IncidentAnalyst analyst) {
        this.statusReader = statusReader;
        this.runbook = runbook;
        this.analyst = analyst;
    }

    // TODO Exercise 3: declare this read-only observation as the lowest-cost action.
    public ServiceObservation observeServices(IncidentRequest request) {
        List<DockerComposeStatus> services = statusReader.statuses();
        List<String> unavailable = services.stream()
                .filter(status -> !isRunning(status.status()))
                .map(DockerComposeStatus::service)
                .toList();
        return new ServiceObservation(request.incidentType(), services, unavailable);
    }

    // TODO Exercise 3: declare this read-only orientation action with cost 0.2.
    public RunbookAssessment applyRunbook(
            IncidentRequest request,
            ServiceObservation observation) {
        return runbook.assess(request, observation);
    }

    @Action(description = "Ask the model for a typed response", readOnly = true, cost = 1.0)
    public IncidentResponseReport analyzeIncident(
            IncidentRequest request,
            ServiceObservation observation,
            RunbookAssessment assessment) {
        return analyst.analyze(request, observation, assessment);
    }

    // TODO Exercise 5: make this the higher-cost action with the same effect
    // as analyzeIncident, then return deterministic runbook output.
    public IncidentResponseReport fallBackToRunbook(
            IncidentRequest request,
            RunbookAssessment assessment) {
        throw new UnsupportedOperationException("TODO Exercise 5: implement deterministic fallback");
    }

    // TODO Exercise 4: declare this read-only action and explicit completion goal.
    public IncidentWorkflowReport prepareReport(
            IncidentRequest request,
            ServiceObservation observation,
            RunbookAssessment assessment,
            IncidentResponseReport response) {
        return new IncidentWorkflowReport(
                "Produce a safe incident response",
                request.incidentType(),
                observation,
                assessment,
                response,
                assessment.requiresApproval());
    }

    private boolean isRunning(String status) {
        String normalized = status.toLowerCase(Locale.ROOT);
        return normalized.contains("running") || normalized.startsWith("up");
    }
}
