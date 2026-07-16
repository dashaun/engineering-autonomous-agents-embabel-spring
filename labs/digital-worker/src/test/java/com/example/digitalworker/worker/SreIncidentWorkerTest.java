package com.example.digitalworker.worker;

import com.example.digitalworker.domain.DockerComposeStatus;
import com.example.digitalworker.domain.IncidentRequest;
import com.example.digitalworker.domain.IncidentWorkflowReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SreIncidentWorkerTest {

    @Test
    void reachesGoalWithTypedActions() {
        ComposeStatusReader statusReader = () -> List.of(
                new DockerComposeStatus("workshop-ollama", "ollama", "ollama", "running", "11434"));
        IncidentAnalyst analyst = (request, observation, assessment) ->
                new com.example.digitalworker.domain.IncidentResponseReport(
                        "Heap is exhausted", assessment.diagnosis(), assessment.recommendedAction());
        var worker = new SreIncidentWorker(statusReader, new IncidentRunbook(), analyst);

        var execution = new BoundedActionPlanner().pursue(
                worker, IncidentRequest.outOfMemory(), IncidentWorkflowReport.class);

        assertThat(execution.achieved()).isTrue();
        assertThat(execution.actions()).containsExactly(
                "observeServices", "applyRunbook", "analyzeIncident", "prepareReport");
        assertThat(execution.result().requiresApproval()).isTrue();
    }

    @Test
    void repairsPlanWhenModelFails() {
        ComposeStatusReader statusReader = List::of;
        IncidentAnalyst analyst = (request, observation, assessment) -> {
            throw new IllegalStateException("Ollama is unavailable");
        };
        var worker = new SreIncidentWorker(statusReader, new IncidentRunbook(), analyst);

        var execution = new BoundedActionPlanner().pursue(
                worker, IncidentRequest.outOfMemory(), IncidentWorkflowReport.class);

        assertThat(execution.failedActions()).singleElement().asString()
                .contains("analyzeIncident", "Ollama is unavailable");
        assertThat(execution.actions()).contains("fallBackToRunbook", "prepareReport");
        assertThat(execution.result().response().analysis()).contains("deterministic policy");
    }
}
