package com.example.digitalworker.worker;

import com.example.digitalworker.domain.IncidentRequest;
import com.example.digitalworker.domain.IncidentType;
import com.example.digitalworker.domain.ServiceObservation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentRunbookTest {

    @Test
    void preservesApprovalPolicyInCode() {
        var assessment = new IncidentRunbook().assess(
                IncidentRequest.outOfMemory(),
                new ServiceObservation(IncidentType.OUT_OF_MEMORY, List.of(), List.of("ollama")));

        assertThat(assessment.diagnosis()).contains("OutOfMemory");
        assertThat(assessment.evidence()).anyMatch(value -> value.contains("ollama"));
        assertThat(assessment.recommendedAction()).contains("heap dump", "canary");
        assertThat(assessment.requiresApproval()).isTrue();
    }
}
