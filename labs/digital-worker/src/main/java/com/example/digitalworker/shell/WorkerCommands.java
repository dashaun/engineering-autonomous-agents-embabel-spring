package com.example.digitalworker.shell;

import com.example.digitalworker.domain.DockerComposeStatus;
import com.example.digitalworker.domain.IncidentRequest;
import com.example.digitalworker.domain.IncidentWorkflowReport;
import com.example.digitalworker.domain.PlanExecution;
import com.example.digitalworker.worker.BoundedActionPlanner;
import com.example.digitalworker.worker.ComposeStatusReader;
import com.example.digitalworker.worker.SreIncidentWorker;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkerCommands {

    private final SreIncidentWorker worker;
    private final BoundedActionPlanner planner;
    private final ComposeStatusReader statusReader;

    public WorkerCommands(
            SreIncidentWorker worker,
            BoundedActionPlanner planner,
            ComposeStatusReader statusReader) {
        this.worker = worker;
        this.planner = planner;
        this.statusReader = statusReader;
    }

    @Command(description = "Resolve an OutOfMemory incident")
    public PlanExecution<IncidentWorkflowReport> oomIncident() {
        return pursue(IncidentRequest.outOfMemory());
    }

    @Command(description = "Resolve a high-latency incident")
    public PlanExecution<IncidentWorkflowReport> latencyIncident() {
        return pursue(IncidentRequest.highLatency());
    }

    @Command(description = "Show every configured Docker Compose service")
    public List<DockerComposeStatus> dockerComposeStatus() {
        return statusReader.statuses();
    }

    private PlanExecution<IncidentWorkflowReport> pursue(IncidentRequest request) {
        return planner.pursue(worker, request, IncidentWorkflowReport.class);
    }
}
