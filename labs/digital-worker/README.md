# Lab: Build the Digital Worker

This is one progressive lab for both 90-minute parts. The starter compiles, but the exercise answers are intentionally missing. Implement each `TODO Exercise` checkpoint, run its focused test, and compare with the answer slide immediately after the exercise.

## Baseline

```bash
./mvnw -DskipTests package
./mvnw test
```

The package command should succeed. The initial test run reports eight tests with three expected errors: one unfinished runbook and two stuck worker plans. All five `BoundedActionPlannerTest` tests already pass. Those failures are the lab work queue, not an environment problem.

## Part 1

### Exercise 1 — DICE and executable policy

Open `IncidentRunbook.assess`. Rebuild the switch so that it:

- collects metrics, logs, and unavailable services as evidence;
- handles both values of `IncidentType`;
- returns a domain-specific recommendation;
- always requires approval.

```bash
./mvnw test -Dtest=IncidentRunbookTest
```

### Exercise 2 — derive the plan from types

Read the action signatures in `SreIncidentWorker`. On paper, start with only `IncidentRequest` and add each action result to a blackboard. Compare your sequence to:

```bash
./mvnw test -Dtest=BoundedActionPlannerTest
```

## Part 2

### Exercise 3 — Spring Beans as actions

Add `@Action` to `observeServices` and `applyRunbook`, including their `readOnly` and `cost` attributes. Run `SreIncidentWorkerTest` and explain why planning was stuck before those capabilities became discoverable.

### Exercise 4 — declare completion

Add `@Action` and `@AchievesGoal` to `prepareReport`. Verify that human approval comes from `RunbookAssessment`, not model output.

### Exercise 5 — repair a failed plan

Rebuild `fallBackToRunbook` as a higher-cost action with the same result type as `analyzeIncident`. Then run:

```bash
./mvnw test -Dtest=SreIncidentWorkerTest#repairsPlanWhenModelFails
```

Before the live run, `./mvnw test` should report all eight tests passing. If you fall behind, copy the implementation from the answer slide and rejoin the next checkpoint.

## Live Run

Complete Exercises 1–5 and confirm all eight tests pass before this section.

From the repository root:

```bash
curl -fsS http://localhost:3000/api/health > /dev/null || \
  docker compose up -d grafana-lgtm
curl -fsS http://localhost:11434/api/tags > /dev/null || \
  docker compose up -d ollama
```

These checks reuse the companion presentation's LGTM stack and Ollama when they are already running, avoiding container-name or port conflicts. Redis remains optional.

From this directory:

```bash
./mvnw spring-boot:run
```

Commands:

```text
docker-compose-status
oom-incident
latency-incident
```

If Ollama is cold or unavailable, the preferred action fails and the planner selects the deterministic runbook action. That is a successful exercise outcome, not a broken workshop.

Spring AI is bounded to two quick connection attempts. Slow inference may still use the five-minute request timeout, but an unavailable endpoint reaches plan repair in seconds.

## Validate OpenTelemetry

Every goal pursuit creates a `digital.worker.plan` observation. Every attempted action creates a child `digital.worker.action` observation. Spring Boot sends their metrics and traces over OTLP; the OpenTelemetry Logback appender sends correlated logs.

After running either incident command, wait about five seconds for the workshop export interval, then open <http://localhost:3000> or run:

```bash
./scripts/validate-telemetry.sh
```

Expected:

```text
metrics: OK (plan_count=..., jvm_threads_live=..., instance=...)
tokens:  OK (input=..., output=..., total=...)
logs:    OK (Plan completed, trace_id=...)
traces:  OK (... spans, ... action spans)
```

To require token usage, first run an incident that completes through `analyzeIncident`, then use:

```bash
REQUIRE_TOKEN_USAGE=true ./scripts/validate-telemetry.sh
```

Token usage comes from the successful model response metadata. A repaired fallback run correctly exports model failure metrics but has no tokens to count.

The `service.instance.id` resource attribute is required by LGTM's provisioned JVM dashboard. LGTM maps it to the Prometheus `instance` label used by the dashboard's Job and Instance variables.

Useful Grafana Explore queries:

```text
Prometheus: digital_worker_action_milliseconds_count{service_name="digital-worker"}
Prometheus: sum by(gen_ai_token_type) (gen_ai_client_token_usage_total{service_name="digital-worker"})
Loki:       {service_name="digital-worker"}
Tempo:      { resource.service.name = "digital-worker" }
```

The local defaults are `http://localhost:4318/v1/{metrics,traces,logs}`. Override them with `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT`, `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`, and `OTEL_EXPORTER_OTLP_LOGS_ENDPOINT`.

## Bonus

Add a third `IncidentType`, its runbook branch, and a test. Do not edit the planner. The worker should discover the same action path for the new domain input.
