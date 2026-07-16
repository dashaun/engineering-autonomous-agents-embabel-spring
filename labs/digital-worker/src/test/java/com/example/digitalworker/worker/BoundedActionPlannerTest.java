package com.example.digitalworker.worker;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundedActionPlannerTest {

    @Test
    void discoversPlanFromJavaTypes() {
        var execution = new BoundedActionPlanner()
                .pursue(new HappyAgent(), new Request(), Report.class);

        assertThat(execution.actions()).containsExactly("observe", "orient", "report");
        assertThat(execution.failedActions()).isEmpty();
        assertThat(execution.result().value()).isEqualTo("observed -> oriented -> complete");
    }

    @Test
    void repairsPlanAfterPreferredActionFails() {
        var execution = new BoundedActionPlanner()
                .pursue(new RepairingAgent(), new Request(), Report.class);

        assertThat(execution.actions()).containsExactly("observe", "fallback", "report");
        assertThat(execution.failedActions()).singleElement().asString().contains("preferred");
        assertThat(execution.result().value()).contains("safe fallback");
    }

    @Test
    void getsStuckWhenRequiredMethodIsNotAnAction() {
        assertThatThrownBy(() -> new BoundedActionPlanner()
                .pursue(new HiddenActionAgent(), new Request(), Report.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Planning is stuck", "Report");
    }

    @Test
    void doesNotCompleteWithoutAnExplicitGoal() {
        assertThatThrownBy(() -> new BoundedActionPlanner()
                .pursue(new MissingGoalAgent(), new Request(), Report.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Planning is stuck", "Report");
    }

    @Test
    void recordsPlanAndActionMetricsIncludingFailureOutcome() {
        var meterRegistry = new SimpleMeterRegistry();
        var observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig().observationHandler(
                new DefaultMeterObservationHandler(meterRegistry));

        new BoundedActionPlanner(observationRegistry)
                .pursue(new RepairingAgent(), new Request(), Report.class);

        assertThat(meterRegistry.get("digital.worker.plan")
                .tag("goal", "Report").timer().count()).isEqualTo(1);
        assertThat(meterRegistry.get("digital.worker.action")
                .tag("action", "preferred")
                .tag("outcome", "failure")
                .timer().count()).isEqualTo(1);
        assertThat(meterRegistry.get("digital.worker.action")
                .tag("action", "fallback")
                .tag("outcome", "success")
                .timer().count()).isEqualTo(1);
    }

    static class HappyAgent {
        @Action(cost = 0.1)
        public Observation observe(Request request) {
            return new Observation("observed");
        }

        @Action(cost = 0.2)
        public Assessment orient(Observation observation) {
            return new Assessment(observation.value() + " -> oriented");
        }

        @Action
        @AchievesGoal(description = "report complete")
        public Report report(Assessment assessment) {
            return new Report(assessment.value() + " -> complete");
        }
    }

    static class RepairingAgent {
        @Action(cost = 0.1)
        public Observation observe(Request request) {
            return new Observation("observed");
        }

        @Action(cost = 1.0)
        public Assessment preferred(Observation observation) {
            throw new IllegalStateException("model unavailable");
        }

        @Action(cost = 10.0)
        public Assessment fallback(Observation observation) {
            return new Assessment("safe fallback");
        }

        @Action
        @AchievesGoal(description = "report complete")
        public Report report(Assessment assessment) {
            return new Report(assessment.value() + " -> complete");
        }
    }

    static class HiddenActionAgent {
        // Intentionally missing @Action: the planner cannot discover this capability.
        public Observation observe(Request request) {
            return new Observation("observed");
        }

        @Action
        @AchievesGoal(description = "report complete")
        public Report report(Observation observation) {
            return new Report(observation.value() + " -> complete");
        }
    }

    static class MissingGoalAgent {
        @Action
        public Report report(Request request) {
            return new Report("result without an explicit goal");
        }
    }

    record Request() {}
    record Observation(String value) {}
    record Assessment(String value) {}
    record Report(String value) {}
}
