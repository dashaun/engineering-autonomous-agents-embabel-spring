package com.example.digitalworker.worker;

import com.example.digitalworker.domain.IncidentRequest;
import com.example.digitalworker.domain.IncidentResponseReport;
import com.example.digitalworker.domain.RunbookAssessment;
import com.example.digitalworker.domain.ServiceObservation;

@FunctionalInterface
public interface IncidentAnalyst {
    IncidentResponseReport analyze(
            IncidentRequest request,
            ServiceObservation observation,
            RunbookAssessment assessment);
}
