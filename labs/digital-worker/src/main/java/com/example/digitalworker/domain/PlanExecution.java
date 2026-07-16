package com.example.digitalworker.domain;

import java.util.List;

public record PlanExecution<T>(
        String goalType,
        boolean achieved,
        int maxSteps,
        List<String> actions,
        List<String> failedActions,
        T result) {

    public PlanExecution {
        actions = List.copyOf(actions);
        failedActions = List.copyOf(failedActions);
    }
}
