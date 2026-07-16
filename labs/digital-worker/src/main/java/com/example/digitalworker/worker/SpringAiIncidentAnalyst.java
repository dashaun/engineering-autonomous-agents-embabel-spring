package com.example.digitalworker.worker;

import com.example.digitalworker.domain.IncidentRequest;
import com.example.digitalworker.domain.IncidentResponseReport;
import com.example.digitalworker.domain.RunbookAssessment;
import com.example.digitalworker.domain.ServiceObservation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class SpringAiIncidentAnalyst implements IncidentAnalyst {

    private final ChatClient chatClient;

    public SpringAiIncidentAnalyst(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public IncidentResponseReport analyze(
            IncidentRequest request,
            ServiceObservation observation,
            RunbookAssessment assessment) {
        return chatClient.prompt().user("""
                You are an SRE following a production runbook.

                Incident: %s
                Metrics: %s
                Logs: %s
                Unavailable services: %s
                Runbook diagnosis: %s
                Approved strategy: %s

                Return concise analysis, diagnosis, and recommendation.
                Keep the recommendation inside the approved strategy.
                """.formatted(
                request.incidentType().displayName(),
                request.metrics(),
                request.logs(),
                observation.unavailableServices(),
                assessment.diagnosis(),
                assessment.recommendedAction()))
                .call()
                .entity(IncidentResponseReport.class);
    }
}
