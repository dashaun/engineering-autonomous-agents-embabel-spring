# AGENTS.md

## Purpose

This repository is a three-hour, two-part workshop about building a typed Digital Worker with Spring Boot, Spring AI, and Embabel. Optimize for a runnable workshop, teaching clarity, and quick participant recovery—not production breadth.

## Version Contract

- Spring Boot `4.1.0`
- Spring AI `2.0.0`
- Embabel `1.0.0-RC1`
- Java 21+ (the RC1 API artifact is Java 21 bytecode)

Do not silently change these versions.

## Structure

- `docs/` — Reveal.js presentation. Exercises are immediately followed by answer slides.
- `labs/digital-worker/` — the single progressive Spring Boot lab used in both parts.
- `docker-compose.yml` — the companion presentation's Ollama, Redis Stack, and Grafana LGTM services.
- `FACILITATOR_GUIDE.md` — timing and recovery notes.
- `QUICK_REFERENCE.md` — participant command and annotation reference.

## Commands

```bash
cd labs/digital-worker
./mvnw -DskipTests package
./mvnw test
./mvnw spring-boot:run
```

```bash
docker compose config --quiet
docker compose up -d
```

```bash
jwebserver -d docs -p 8000
```

## Architecture

The lab uses one SRE incident workflow:

```text
IncidentRequest
  → ServiceObservation
  → RunbookAssessment
  → IncidentResponseReport
  → IncidentWorkflowReport
```

Participants add real Embabel RC1 `@Action` and `@AchievesGoal` annotations to the `@Agent`-annotated `SreIncidentWorker`. `BoundedActionPlanner` is a deliberately small teaching runner that exposes the blackboard, action applicability, cost, bounded execution, and replanning mechanics.

Embabel usually infers data preconditions and effects from method parameter and return types. Do not introduce fictional `@Precondition`, `@Effects`, `@Effect`, or `@Provided` APIs.

The lower-cost model action is preferred. A higher-cost deterministic action produces the same result type and is selected when the model action fails. Human approval comes from Java runbook policy, never model output.

## Modification Rules

- Keep one domain and one progressive lab unless the workshop duration changes.
- Preserve the companion presentation's direct, Spring-developer voice and green/dark visual system.
- Keep code examples synchronized with compiling lab source.
- Keep answer slides immediately after exercises.
- Keep exercise answers out of starter source. It must compile, but its checkpoint tests intentionally start red.
- Keep all tests independent of Docker and the model; the live run is optional.
- Every external failure must be visible in `PlanExecution.failedActions`.
- Never add an unbounded agent loop.
