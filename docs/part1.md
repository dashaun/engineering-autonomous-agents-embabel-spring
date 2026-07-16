<!-- .slide: data-background-color="#191e1e" -->

# Part 1

## From Prompting to Planning

90 minutes

Notes:
Minute 10.
Use Right Arrow to enter this horizontal module, then Down Arrow for its vertical sequence. Press Right at any point to jump directly to Part 2.

Likely questions:
- Q: Why a two-dimensional deck? A: Each horizontal column is a workshop module; vertical movement follows its teaching sequence without losing shortcut navigation.

---

## Enter Embabel

Embabel brings a disciplined planning model to JVM agents:

- **Actions** — steps the worker is allowed to take
- **Conditions** — what must be true before a step
- **Goals** — explicit completion conditions
- **Blackboard** — typed working memory for one process
- **Plan** — a path discovered from the current state

> The worker chooses the path. Your code defines the world.

Notes:
Minute 10-14.
This repeats the companion talk on purpose, then immediately maps it to code.

Likely questions:
- Q: Is the LLM the planner? A: No. The planning model selects declared actions from state and goals; an individual action may use an LLM.
- Q: Can the worker execute anything it invents? A: No. It can only select capabilities supplied by application code.
- Q: Is Embabel Java-only? A: It is a JVM framework written in Kotlin with natural Java and Kotlin authoring models.

---

## OODA Is the Runtime Rhythm

```text
OBSERVE ──→ ORIENT ──→ DECIDE ──→ ACT
   ↑                                  │
   └──────────── re-observe ──────────┘
```

| PHASE | WORKSHOP ACTION |
|---|---|
| Observe | read Docker Compose state |
| Orient | apply executable runbook policy |
| Decide | request a typed recommendation |
| Act | prepare an approval-required report |

Each result changes what is possible next.

Notes:
Minute 14-18.
OODA is not four API interfaces. It is a useful way to reason about the feedback loop.

Likely questions:
- Q: Does every Embabel action map to exactly one OODA phase? A: No. The labels explain the decision rhythm; real actions may combine concerns.
- Q: What causes the loop? A: After an action produces state—or fails—the planner reassesses what is applicable and whether the goal is satisfied.

---

## GOAP Starts With the Outcome

```java
planner.pursue(
    worker,
    IncidentRequest.outOfMemory(),
    IncidentWorkflowReport.class);
```

The caller supplies:

1. a starting state;
2. available actions;
3. a desired goal type.

The caller does **not** name the action sequence.

Notes:
Minute 18-21.
Contrast this with a service method that calls four collaborators in a fixed order.

Likely questions:
- Q: Does GOAP plan backward or forward? A: Conceptually it reasons from the goal and required conditions; execution proceeds forward from current state.
- Q: Is the order random? A: No. Applicability, cost, goal conditions, and planner policy determine the path.
- Q: Can a normal controller invoke a goal? A: Yes. Focused execution can begin from a controller, listener, command, or scheduled job.

---

## Types Are Preconditions and Effects

```java
@Action
public RunbookAssessment applyRunbook(
        IncidentRequest request,
        ServiceObservation observation) {
    return runbook.assess(request, observation);
}
```

```text
Preconditions: IncidentRequest + ServiceObservation exist
Effect:        RunbookAssessment now exists
```

Embabel infers most conditions from method parameters and return types.

Notes:
Minute 21-25.
Do not teach fictional @Precondition or @Effects annotations. RC1 also exposes explicit pre/post condition names, but data flow is the clearest starting point.

Likely questions:
- Q: Where are preconditions declared in this example? A: The required method-parameter types must exist on the blackboard before the action is applicable.
- Q: Where are effects declared? A: The return type becomes new typed state. RC1 also supports named pre/post conditions when type flow is not enough.
- Q: What about boolean guards such as retryCount? A: Use explicit conditions or domain types that encode the guarded state; do not rely on prompt prose.

---

## The Blackboard Is Typed Working Memory

```text
Start     IncidentRequest
Observe   + ServiceObservation
Orient    + RunbookAssessment
Decide    + IncidentResponseReport
Act       + IncidentWorkflowReport  ← goal
```

No JSON router. No stringly typed state machine.

The debugger, compiler, tests, logs, and planner all see the same domain objects.

Notes:
Minute 25.
Connect this directly to the companion presentation's “Blackboard Is Typed Working Memory” slide. The workshop runner keeps the map visible for teaching.

Likely questions:
- Q: Is the blackboard a database? A: No. It is process working memory. Persistence and recovery are separate application concerns.
- Q: What if two objects have the same class? A: This deliberately small runner stores one value per class. The full platform has richer bindings and conditions.
- Q: Is the blackboard sent wholesale to the LLM? A: No. An action explicitly chooses which domain facts enter its prompt.

---

## DICE: Context Is a Domain Model

```text
IncidentRequest       what happened
ServiceObservation    what is true now
RunbookAssessment     what the organization permits
        ↓
Spring AI             reason inside those boundaries
        ↓
IncidentResponseReport
```

**Domain-Integrated Context Engineering** keeps policy in testable Java and gives the model the facts it needs.

Notes:
Minute 25-29.
This is beyond retrieval. Context includes behavior, live state, enums, invariants, and policy.

Likely questions:
- Q: Is DICE a replacement for RAG? A: No. Retrieval can supply knowledge; DICE integrates retrieved knowledge with live state, domain types, behavior, and policy.
- Q: Why not pass JSON? A: Java types provide compiler-checked semantics and domain methods before serialization ever reaches a model.
- Q: Does the model enforce approval? A: No. Java policy does. The model cannot change `requiresApproval`.

---

## Lab Setup — 8 Minutes

```bash
cd labs/digital-worker
./mvnw -DskipTests package
./mvnw test
```

Find these files:

```text
domain/IncidentRequest.java
domain/ServiceObservation.java
domain/RunbookAssessment.java
worker/IncidentRunbook.java
worker/SreIncidentWorker.java
worker/BoundedActionPlanner.java
```

The starter should compile. The initial suite has three expected errors at unfinished TODOs; all five planner tests already pass.

Notes:
Minute 29-37.
Pair people while Maven resolves. If dependencies are already cached this takes seconds. Use mvnw.cmd on Windows.

Likely questions:
- Q: Do the tests require Docker or Ollama? A: No. The tests use deterministic collaborators and run offline after dependencies are cached; the telemetry assertion uses an in-memory registry.
- Q: How do I distinguish an expected failure from setup trouble? A: The starter package must compile. Test failures should point at `TODO Exercise 1` or the planner being stuck before the unfinished worker actions and goal.
- Q: Which IDE is required? A: None. Any Java 21-capable IDE or editor works.

---

## Exercise 1 — Build DICE (15 Minutes)

Open `IncidentRunbook.assess`.

1. Put metrics, logs, and unavailable services into `evidence`.
2. Use the `IncidentType` enum to select policy.
3. Require approval for every proposed production change.
4. Run:

```bash
./mvnw test -Dtest=IncidentRunbookTest
```

Do the exercise first. The answer is next.

Notes:
Minute 37-52.
Have participants replace the TODO exception with the typed policy. The next slide is the recovery path for late arrivals.

Likely questions:
- Q: Where is the answer? A: The starter source contains only the TODO. The next slide is authoritative if participants need to catch up.
- Q: What is the learning objective? A: Encode organizational knowledge and invariants in domain code before asking a model to reason.
- Q: Can I add another incident type? A: Yes; that is the bonus exercise and should require a domain branch and test, not planner edits.

---

## Answer 1 — Executable Domain Policy

```java
var evidence = new ArrayList<String>();
evidence.add(request.metrics());
evidence.add(request.logs());
evidence.add("Unavailable services: " +
    observation.unavailableServices());

return switch (request.incidentType()) {
    case OUT_OF_MEMORY -> new RunbookAssessment(
        "Heap pressure is consistent with an OutOfMemory failure.",
        evidence,
        "Capture a heap dump, roll back the latest risky change, " +
            "then validate with a canary.",
        true);
    case HIGH_LATENCY -> new RunbookAssessment(
        "Database timeouts and cache misses indicate " +
            "dependency saturation.",
        evidence,
        "Check database and cache health, then use an approved " +
            "rollback or scale-out.",
        true);
};
```

Notes:
Minute 52-57. The important answer is not the prose; it is that policy remains deterministic and typed.

Likely questions:
- Q: Why keep evidence as strings? A: It is the smallest useful lab model. A production domain can introduce typed Metric, LogEvidence, and ServiceHealth values.
- Q: Why is approval always true? A: The worker only prepares remediation advice; production mutation remains a human decision.
- Q: Could the LLM write the runbook branch? A: It can assist development, but reviewed Java code remains the runtime authority.

---

## Exercise 2 — Read the Action Graph (15 Minutes)

Without running the worker, write down the plan from these signatures:

```java
ServiceObservation observeServices(IncidentRequest request)

RunbookAssessment applyRunbook(
    IncidentRequest request, ServiceObservation observation)

IncidentResponseReport analyzeIncident(
    IncidentRequest request,
    ServiceObservation observation,
    RunbookAssessment assessment)

IncidentWorkflowReport prepareReport(
    IncidentRequest request,
    ServiceObservation observation,
    RunbookAssessment assessment,
    IncidentResponseReport response)
```

Then run `./mvnw test -Dtest=BoundedActionPlannerTest`.

Notes:
Minute 57-72.
Ask participants to mark each input type as it becomes available.

Likely questions:
- Q: Why does `prepareReport` take every intermediate value? A: Those parameters make its prerequisites explicit and preserve evidence in the final report.
- Q: Could actions run in parallel? A: Independent actions can be parallelized by a richer runtime; this teaching graph is intentionally linear.
- Q: What happens if no action is applicable? A: Planning is stuck and fails visibly rather than inventing a capability.

---

## Answer 2 — The Plan Is Data Flow

```text
IncidentRequest
   │
   ├── observeServices ──→ ServiceObservation
   │                              │
   ├──────── applyRunbook ────────┴─→ RunbookAssessment
   │                                      │
   ├──────────── analyzeIncident ─────────┴─→ IncidentResponseReport
   │                                                 │
   └──────────────── prepareReport ──────────────────┴─→ GOAL
```

Adding a result to the blackboard makes downstream actions applicable.

Notes:
Minute 72-77.
The full Embabel runtime performs richer planning. The small workshop runner makes the mechanism visible while preserving the RC1 annotations and model.

Likely questions:
- Q: Is `BoundedActionPlanner` the Embabel production planner? A: No. It is the same transparent teaching runner used by the companion presentation to expose action/condition/goal mechanics with this version matrix.
- Q: Then where is Embabel used? A: The worker uses RC1's real `@Agent`, `@Action`, `@AchievesGoal`, action cost, and retry-policy API.
- Q: Why not hide the mechanics? A: The workshop goal is to teach the planning mindset, not merely demonstrate magic.

---

## The Planner Must Be Bounded

```java
static final int MAX_STEPS = 8;

for (int step = 0; step < MAX_STEPS; step++) {
    Method next = actions.stream()
        .filter(action -> canRun(action, blackboard, attempted))
        .findFirst()
        .orElseThrow();

    Object result = invoke(next, agent, blackboard);
    blackboard.put(result.getClass(), result);
}
```

Two guardrails: actions do not rerun by default, and the process has a hard step limit.

Notes:
Minute 77-82.
Never demonstrate an unbounded autonomous loop on stage.

Likely questions:
- Q: Why eight steps? A: It is comfortably above the five-attempt failure path and small enough to expose runaway behavior quickly.
- Q: Can an action rerun? A: Yes when explicitly permitted, but the workshop agent uses FIRE_ONCE semantics to make progress and failure easy to audit.
- Q: Is a timeout also required? A: Yes in production. The lab includes a five-minute model request timeout; plan steps and external calls need separate bounds.

---

## Part 1 Checkpoint

You can now explain:

- OODA as re-observation after every action;
- GOAP as finding a path to a goal;
- DICE as typed facts plus executable domain policy;
- parameters as preconditions and results as effects;
- the blackboard as inspectable working memory.

**Take a break. Part 2 turns the graph into a Digital Worker.**

Notes:
Minute 82-90.
Use the remaining time to get every participant to green tests. Then take the scheduled break outside the three-hour instructional clock if the event allows.

Likely questions:
- Q: What must be working before Part 2? A: Maven tests, the runbook exercise, and the ability to explain the four-action type graph.
- Q: Should Docker be running now? A: It is optional until the live run near the end of Part 2.
