<!-- .slide: data-background-color="#191e1e" -->

# Part 2

## Building the Digital Worker

90 minutes

Notes:
Part 2, minute 0.
Use Right Arrow from any Part 1 slide to jump here. Down Arrow follows the build sequence; Up Arrow returns to an earlier answer without leaving Part 2.

Likely questions:
- Q: Can I skip directly to the live run? A: Press Esc and choose the Part 2 column, then the “Run the Digital Worker” row.

---

## Our Contract With the Worker

```text
We allow it to:
  observe Compose
  apply our runbook
  ask a model for a typed recommendation
  fall back to deterministic policy
  prepare a report for human approval

We do not allow it to:
  invent shell commands
  restart production
  bypass approval
  run forever
```

Autonomy is meaningful only inside explicit boundaries.

Notes:
Minute 0-5.

Likely questions:
- Q: Is a read-only worker still autonomous? A: Yes. Autonomy is goal-directed selection and execution inside its capability boundary, not unrestricted mutation.
- Q: Why stop at human approval? A: Incident remediation is high impact; the workshop demonstrates useful bounded autonomy without normalizing unsupervised production changes.
- Q: Where would authorization live? A: In application services, explicit conditions, identity, and policy—not in the prompt.

---

## Spring Still Owns the Components

```java
@Component
@Agent(
    name = "sre-incident-worker",
    description = "Produce a bounded incident response",
    actionRetryPolicy = ActionRetryPolicy.FIRE_ONCE)
public class SreIncidentWorker {

    private final ComposeStatusReader statusReader;
    private final IncidentRunbook runbook;
    private final IncidentAnalyst analyst;
}
```

Constructor injection, interfaces, tests, configuration, and observability work exactly as they do in the rest of Spring.

Notes:
Minute 5-9.

Likely questions:
- Q: Does Embabel replace Spring? A: No. Spring still creates and wires beans; Embabel describes which methods are agent capabilities and how goals are reached.
- Q: Can repositories and transactional services be actions? A: Yes, with the same caution, testing, authorization, and idempotency requirements as any application entry point.
- Q: What does FIRE_ONCE mean? A: An action is not repeatedly retried inside the process unless an explicit policy allows it.

---

## Exercise 3 — Turn Beans Into Actions (18 Minutes)

In `SreIncidentWorker`:

1. annotate `observeServices` and `applyRunbook` with `@Action`;
2. mark them `readOnly = true`;
3. give observation a lower cost than orientation;
4. predict what breaks if `observeServices` is not an action;
5. run `./mvnw test -Dtest=SreIncidentWorkerTest`.

The answer is next.

Notes:
Minute 9-27.
Have participants run the red test, read the planner's stuck message, then add the two annotations from the requirements.

Likely questions:
- Q: Does removing `@Action` cause a compilation error? A: No. It removes the method from the planner's capability surface, so the runtime plan becomes impossible.
- Q: What does `readOnly = true` do here? A: It truthfully describes the capability to Embabel and observability tooling; the application service still determines actual side effects.
- Q: Why assign costs 0.1 and 0.2? A: Costs make preference explicit when multiple applicable paths exist. They are not timing estimates.

---

## Answer 3 — Observe and Orient

```java
@Action(
    description = "Observe Docker Compose services",
    readOnly = true,
    cost = 0.1)
public ServiceObservation observeServices(IncidentRequest request) {
    List<DockerComposeStatus> services = statusReader.statuses();
    List<String> unavailable = services.stream()
        .filter(status -> !isRunning(status.status()))
        .map(DockerComposeStatus::service)
        .toList();
    return new ServiceObservation(
        request.incidentType(), services, unavailable);
}

@Action(description = "Apply the production runbook",
        readOnly = true, cost = 0.2)
public RunbookAssessment applyRunbook(
        IncidentRequest request, ServiceObservation observation) {
    return runbook.assess(request, observation);
}
```

Notes:
Minute 27. Emphasize that these remain ordinary Spring methods. The annotation adds planning metadata; it does not move business logic into a framework DSL.

Likely questions:
- Q: Why is observation an action rather than a `@Tool`? A: In this Embabel flow it is a planned, typed, read-only step. Spring AI tools are model-callable functions and solve a different coordination problem.
- Q: Is `DockerComposeService` mocked in tests? A: The worker depends on `ComposeStatusReader`, so tests provide a deterministic lambda without Docker.
- Q: Why filter running status in the worker? A: It turns infrastructure output into domain-oriented `ServiceObservation` state.

---

## DICE Enters the Model Call

```java
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
        request.metrics(), request.logs(),
        observation.unavailableServices(),
        assessment.diagnosis(), assessment.recommendedAction()))
    .call()
    .entity(IncidentResponseReport.class);
```

The model reasons. Java owns the schema and policy.

Notes:
Minute 27-32.

Likely questions:
- Q: Is `.entity(...)` guaranteed to return valid data? A: It requests structured mapping, but providers can still fail or return invalid output; that is why the action can fail and the plan has a fallback.
- Q: Why include both raw metrics and runbook diagnosis? A: Metrics ground the model in evidence; the diagnosis and approved strategy constrain its interpretation.
- Q: Is this chain-of-thought? A: No. The application requests a business result object, not private reasoning traces.

---

## Exercise 4 — Complete the Goal (13 Minutes)

Annotate the terminal action:

```java
public IncidentWorkflowReport prepareReport(
    IncidentRequest request,
    ServiceObservation observation,
    RunbookAssessment assessment,
    IncidentResponseReport response) { ... }
```

Requirements:

- the method is an action;
- the result is an explicit goal;
- `requiresApproval` comes from the runbook, never the model.

Notes:
Minute 32-45.

Likely questions:
- Q: Why is a return type not enough to finish? A: The goal annotation distinguishes a terminal business outcome from ordinary intermediate state.
- Q: Can an agent have multiple goals? A: Yes. This workshop uses one explicit goal to keep the planning graph teachable.
- Q: Should a goal method mutate production? A: Not here. It assembles the approval artifact; any later mutation would be a separately guarded capability.

---

## Answer 4 — Explicit Completion

```java
@Action(description = "Prepare the approval-required report",
        readOnly = true)
@AchievesGoal(description =
    "A safe incident response is ready for human approval")
public IncidentWorkflowReport prepareReport(
        IncidentRequest request,
        ServiceObservation observation,
        RunbookAssessment assessment,
        IncidentResponseReport response) {
    return new IncidentWorkflowReport(
        "Produce a safe incident response",
        request.incidentType(), observation,
        assessment, response,
        assessment.requiresApproval());
}
```

Producing a report is not enough. The goal annotation tells the process it may stop.

Notes:
Minute 45. Point out that `requiresApproval` comes from `RunbookAssessment`, not `IncidentResponseReport`.

Likely questions:
- Q: Could the model set approval to false? A: No. The model output type does not contain that field, and the goal copies the deterministic runbook value.
- Q: Why retain every intermediate object in the final report? A: It makes the outcome explainable and gives auditors the evidence, policy, recommendation, and completion status together.
- Q: Does `@AchievesGoal` make the method an action automatically? A: Keep both annotations explicit in this Java example: one declares capability, the other completion.

---

## Failure Is New Information

```text
Preferred plan
  analyzeIncident (cost 1) ──X model unavailable

Re-observe
  IncidentRequest + ServiceObservation + RunbookAssessment still exist

Repaired plan
  fallBackToRunbook (cost 10) ──→ IncidentResponseReport
  prepareReport ──→ goal
```

Plan repair does not ask the model to improvise. It chooses another declared capability.

Notes:
Minute 45-50.

Likely questions:
- Q: Is this the same failure handling as the companion? A: It is the next iteration. The companion retained deterministic policy on model failure; the workshop expresses that fallback as an alternate typed action so participants can see repair.
- Q: Does replanning mean retrying the same prompt? A: No. The failed preferred action is recorded and a different declared action becomes the path.
- Q: What state survives failure? A: The request, observation, and runbook assessment already on the blackboard.

---

## Exercise 5 — Add a Repair Path (15 Minutes)

Create a second action that returns `IncidentResponseReport`:

- inputs: `IncidentRequest` and `RunbookAssessment`;
- no model call;
- cost: `10.0` so it is not preferred;
- preserve the diagnosis and recommendation;
- make the failure visible in the audit result.

Run:

```bash
./mvnw test -Dtest=SreIncidentWorkerTest#repairsPlanWhenModelFails
```

Notes:
Minute 50-65.
Participants can force failure by making the IncidentAnalyst test double throw.

Likely questions:
- Q: Why does fallback omit `ServiceObservation`? A: The deterministic response needs only the request and runbook; fewer prerequisites also keep it available when model-specific context is unusable.
- Q: Could both actions run? A: Once either produces `IncidentResponseReport`, the other no longer adds a missing effect and is skipped.
- Q: What should participants assert? A: The preferred action appears in `failedActions`, fallback appears in completed `actions`, and the goal is still achieved.

---

## Answer 5 — Deterministic Fallback

```java
@Action(
    description = "Fall back to deterministic runbook output",
    readOnly = true,
    cost = 10.0)
public IncidentResponseReport fallBackToRunbook(
        IncidentRequest request,
        RunbookAssessment assessment) {
    return new IncidentResponseReport(
        "The model was unavailable; deterministic policy was retained.",
        assessment.diagnosis(),
        assessment.recommendedAction());
}
```

Both actions have the same effect type. Cost prefers the model; the blackboard makes fallback applicable after failure.

Notes:
Minute 65. This is the key plan-repair answer. Compare the two action signatures and circle their identical result type.

Likely questions:
- Q: Why cost 10 rather than a boolean “fallback” flag? A: Cost expresses general path preference without hard-coding an if/else route.
- Q: Is the fallback response AI-generated? A: No. It carries reviewed runbook diagnosis and action into the same typed result contract.
- Q: Can a production planner consider risk as well as cost? A: Yes. Utility, policy, and dynamic cost/value methods can express richer selection.

---

## The Repair Loop

```java
try {
    Object result = invoke(next, agent, blackboard);
    completed.add(next.getName());
    blackboard.put(result.getClass(), result);
} catch (RuntimeException exception) {
    failed.add(next.getName() + ": " + exception.getMessage());
    // loop: reassess current state and choose another action
}
```

The failed action is not silently retried. The next plan uses the facts that remain true.

Notes:
Minute 65-69.

Likely questions:
- Q: Why catch `RuntimeException` at the planner boundary? A: A failed capability is process information. The runner records it, preserves valid state, and checks for another path.
- Q: Are all exceptions recoverable? A: No. If no alternate action can reach the goal, the process fails visibly. Production code should classify retryable, recoverable, and fatal failures.
- Q: Will the failed action run again? A: No. It is in the attempted set and the agent uses FIRE_ONCE semantics.

---

## Every Execution Explains Itself

```java
public record PlanExecution<T>(
    String goalType,
    boolean achieved,
    int maxSteps,
    List<String> actions,
    List<String> failedActions,
    T result
) {}
```

```text
actions:       observeServices, applyRunbook,
               fallBackToRunbook, prepareReport
failedActions: analyzeIncident: Ollama is unavailable
achieved:      true
```

An audit trail should record decisions and outcomes, not private chain-of-thought.

Notes:
Minute 69. Read the two lists aloud: completed actions and failed actions. That distinction is more useful operationally than a generic success flag.

Likely questions:
- Q: Where would this audit data go in production? A: Structured logs, traces, process persistence, and an organization-approved audit store.
- Q: Why not log every prompt and response? A: They may contain sensitive data and do not replace action/outcome telemetry. Apply explicit redaction and retention policy.
- Q: Can this resume after a restart? A: Not in the small in-memory runner. Durable process recovery is a separate production concern.

---

## Run the Digital Worker (10 Minutes)

From the repository root:

```bash
curl -fsS http://localhost:3000/api/health > /dev/null || \
  docker compose up -d grafana-lgtm
curl -fsS http://localhost:11434/api/tags > /dev/null || \
  docker compose up -d ollama
```

Then:

```bash
cd labs/digital-worker
./mvnw spring-boot:run
```

At the shell prompt:

```text
docker-compose-status
oom-incident
latency-incident
```

Ollama can be cold. Slow inference has a five-minute request timeout, while connection failures get two quick attempts before plan repair.

Notes:
Minute 69-79.
If Docker is unavailable, demonstrate the unit-tested path. Do not lose the workshop to infrastructure.

Likely questions:
- Q: Why preflight each service? A: It reuses the companion session's containers when present and only asks Compose to create what is missing.
- Q: What if I attended the companion session and its containers are still running? A: Reuse them. The preflight command checks port 11434 before asking Compose to start Ollama.
- Q: Why run `docker-compose-status` first? A: It proves the worker can observe every configured service, including stopped or uncreated ones, before any model call.
- Q: How long is the first model call? A: It depends on hardware and model warm-up. Slow inference has a five-minute request timeout, but unavailable connections use two short attempts so repair happens quickly.
- Q: What if the model fails? A: That is a valid demonstration: the trace should show `analyzeIncident` failing and `fallBackToRunbook` completing the goal.
- Q: Why is the JVM dashboard empty? A: Keep the worker running, select Job `embabel-workshop/digital-worker`, then select the current Instance. CPU, heap, GC, classes, and threads populate; HTTP panels stay empty because this is a non-web Spring Shell application.

---

## Final Validation

```bash
./mvnw test
# after an incident and one five-second export interval
./scripts/validate-telemetry.sh
```

Eight tests prove:

- the runbook retains deterministic approval policy;
- types determine action applicability;
- undiscoverable methods cannot satisfy a plan;
- a result is not completion without an explicit goal;
- the planner reaches a declared goal;
- execution is bounded;
- model failure produces a repaired, auditable plan.

The live check proves metrics, correlated logs, and action spans reached LGTM.

Notes:
Minute 79-84.

Likely questions:
- Q: Why integration-style behavior tests without `@SpringBootTest`? A: The core planning semantics are faster and more deterministic when assembled directly; application startup is validated separately.
- Q: What is not covered? A: Authentication, durable process state, load behavior, model evaluation, and production deployment are intentionally outside three hours.
- Q: Do passing tests prove the model is good? A: No. They prove orchestration, invariants, and recovery. Model quality needs a separate evaluation set.
- Q: What exactly does the telemetry script query? A: Prometheus for the plan timer, Loki for the correlated completion log and trace ID, then Tempo for that exact trace.
- Q: Where is token usage? A: After a successful model call, query `sum by(gen_ai_token_type) (gen_ai_client_token_usage_total{service_name="digital-worker"})`; `REQUIRE_TOKEN_USAGE=true ./scripts/validate-telemetry.sh` enforces input, output, and total counters for the latest run's instance.

---

## Part 2 Checkpoint

You built a worker that:

- navigates a typed domain;
- invokes real Spring-managed services;
- plans from preconditions and effects;
- stops at an explicit goal;
- repairs a failed preferred path;
- preserves human approval;
- leaves an audit trail.

That is the move from **calling APIs** to **building workers**.

Notes:
Minute 84-86. Use the final four minutes for the production blueprint and close.

Likely questions:
- Q: What should I build first at work? A: One valuable, bounded goal with observable read-only actions and a human-reviewed outcome.
- Q: How do I extend this lab? A: Add an incident type, a new observation source, or an alternate typed action without editing the planner.
