package com.example.digitalworker.worker;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.example.digitalworker.domain.PlanExecution;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A deliberately small, visible GOAP runner for the workshop.
 * Embabel's production runtime uses the same action/condition/goal model.
 */
@Component
public class BoundedActionPlanner {

    static final int MAX_STEPS = 8;
    private static final Logger logger = LoggerFactory.getLogger(BoundedActionPlanner.class);

    private final ObservationRegistry observationRegistry;

    /** Keeps the focused unit tests independent of an application context. */
    BoundedActionPlanner() {
        this(ObservationRegistry.NOOP);
    }

    @Autowired
    public BoundedActionPlanner(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    public <T> PlanExecution<T> pursue(Object agent, Object input, Class<T> goalType) {
        Observation plan = Observation.createNotStarted(
                        "digital.worker.plan", observationRegistry)
                .contextualName("pursue " + goalType.getSimpleName())
                .lowCardinalityKeyValue("agent", agent.getClass().getSimpleName())
                .lowCardinalityKeyValue("goal", goalType.getSimpleName());

        return plan.observe(() -> executePlan(agent, input, goalType));
    }

    private <T> PlanExecution<T> executePlan(Object agent, Object input, Class<T> goalType) {
        Map<Class<?>, Object> blackboard = new LinkedHashMap<>();
        blackboard.put(input.getClass(), input);

        List<Method> actions = Arrays.stream(agent.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(Action.class))
                .sorted(Comparator.comparingDouble(
                        method -> method.getAnnotation(Action.class).cost()))
                .toList();
        Set<Method> attempted = new LinkedHashSet<>();
        List<String> completed = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        logger.atInfo()
                .addKeyValue("goal", goalType.getSimpleName())
                .addKeyValue("input.type", input.getClass().getSimpleName())
                .log("Plan started");

        for (int step = 0; step < MAX_STEPS; step++) {
            Method next = actions.stream()
                    .filter(method -> canRun(method, blackboard, attempted))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Planning is stuck before reaching " + goalType.getSimpleName()));

            attempted.add(next);
            try {
                Object result = invokeObserved(next, agent, blackboard);
                completed.add(next.getName());
                blackboard.put(result.getClass(), result);

                if (goalType.isInstance(result)
                        && next.isAnnotationPresent(AchievesGoal.class)) {
                    logger.atInfo()
                            .addKeyValue("goal", goalType.getSimpleName())
                            .addKeyValue("actions.completed", completed.size())
                            .addKeyValue("actions.failed", failed.size())
                            .log("Plan completed");
                    return new PlanExecution<>(
                            goalType.getSimpleName(),
                            true,
                            MAX_STEPS,
                            completed,
                            failed,
                            goalType.cast(result));
                }
            } catch (RuntimeException exception) {
                // Re-observe the blackboard and choose another applicable action.
                failed.add(next.getName() + ": " + exception.getMessage());
            }
        }
        throw new IllegalStateException(
                "Step limit reached before achieving " + goalType.getSimpleName());
    }

    private Object invokeObserved(
            Method method,
            Object agent,
            Map<Class<?>, Object> blackboard) {
        Observation action = Observation.createNotStarted(
                        "digital.worker.action", observationRegistry)
                .contextualName("action " + method.getName())
                .lowCardinalityKeyValue("action", method.getName())
                .lowCardinalityKeyValue(
                        "effect", method.getReturnType().getSimpleName());

        action.start();
        try (Observation.Scope ignored = action.openScope()) {
            try {
                logger.atInfo()
                        .addKeyValue("action", method.getName())
                        .log("Action started");
                Object result = invoke(method, agent, blackboard);
                action.lowCardinalityKeyValue("outcome", "success");
                logger.atInfo()
                        .addKeyValue("action", method.getName())
                        .addKeyValue("effect", result.getClass().getSimpleName())
                        .log("Action completed");
                return result;
            } catch (RuntimeException exception) {
                action.error(exception);
                action.lowCardinalityKeyValue("outcome", "failure");
                logger.atWarn()
                        .addKeyValue("action", method.getName())
                        .addKeyValue("error.type", exception.getClass().getSimpleName())
                        .addKeyValue("error.message", exception.getMessage())
                        .log("Action failed; planner will replan");
                throw exception;
            }
        } finally {
            action.stop();
        }
    }

    private boolean canRun(
            Method method,
            Map<Class<?>, Object> blackboard,
            Set<Method> attempted) {
        Action action = method.getAnnotation(Action.class);
        if (attempted.contains(method) && !action.canRerun()) {
            return false;
        }
        if (blackboard.values().stream().anyMatch(method.getReturnType()::isInstance)) {
            return false;
        }
        return Arrays.stream(method.getParameterTypes())
                .allMatch(required -> blackboard.values().stream().anyMatch(required::isInstance));
    }

    private Object invoke(Method method, Object agent, Map<Class<?>, Object> blackboard) {
        Object[] arguments = Arrays.stream(method.getParameterTypes())
                .map(required -> blackboard.values().stream()
                        .filter(required::isInstance)
                        .findFirst()
                        .orElseThrow())
                .toArray();
        try {
            Object result = method.invoke(agent, arguments);
            if (result == null) {
                throw new IllegalStateException("Action returned null: " + method.getName());
            }
            return result;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot invoke " + method.getName(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Action failed: " + method.getName(), cause);
        }
    }
}
