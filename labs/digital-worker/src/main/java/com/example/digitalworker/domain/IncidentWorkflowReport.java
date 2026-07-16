package com.example.digitalworker.domain;

public record IncidentWorkflowReport(
        String goal,
        IncidentType incidentType,
        ServiceObservation observation,
        RunbookAssessment runbookAssessment,
        IncidentResponseReport response,
        boolean requiresApproval) {
}
