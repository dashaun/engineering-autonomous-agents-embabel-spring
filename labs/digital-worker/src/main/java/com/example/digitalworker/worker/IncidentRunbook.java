package com.example.digitalworker.worker;

import com.example.digitalworker.domain.IncidentRequest;
import com.example.digitalworker.domain.RunbookAssessment;
import com.example.digitalworker.domain.ServiceObservation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class IncidentRunbook {

    // TODO Exercise 1: build the evidence list and typed policy switch.
    // The complete implementation is on the Answer 1 slide.
    public RunbookAssessment assess(IncidentRequest request, ServiceObservation observation) {
        throw new UnsupportedOperationException("TODO Exercise 1: implement runbook policy");
    }
}
