# Engineering Autonomous Agents with Embabel and Spring

A code-heavy, three-hour workshop delivered in two 90-minute parts. It is the hands-on companion to `agentic-ai-workflows-with-embabel-1.0` and uses the same SRE Digital Worker, visual language, Docker infrastructure, and teaching voice.

The outcome is a working worker that observes a real Compose project, applies typed Java policy, requests a structured Spring AI result, reaches an Embabel goal, and repairs its plan when the model action fails.

## Version Contract

| Technology | Version |
|---|---:|
| Spring Boot | `4.1.0` |
| Spring AI | `2.0.0` |
| Embabel | `1.0.0-RC1` |
| Java | `21+` |

Spring Boot 4.1 supports Java 17, but the published Embabel RC1 API artifact is Java 21 bytecode. Use JDK 21 or newer for this workshop.

## Workshop Schedule

### Part 1 — From Prompting to Planning (90 minutes)

| Time | Activity |
|---:|---|
| 0-10 | Welcome, outcome, stack, setup |
| 10-29 | OODA, GOAP, typed blackboard, DICE |
| 29-37 | Lab setup and baseline tests |
| 37-57 | Exercise 1: executable domain policy |
| 57-77 | Exercise 2: read the action graph |
| 77-90 | bounded planning and checkpoint |

### Part 2 — Building the Digital Worker (90 minutes)

| Time | Activity |
|---:|---|
| 0-9 | worker boundaries and Spring components |
| 9-32 | Exercise 3: Spring Beans as actions |
| 32-45 | Exercise 4: explicit completion goal |
| 45-69 | Exercise 5: failure and plan repair |
| 69-84 | run the worker and validate |
| 84-90 | production blueprint and close |

## Quick Start

Verify the starter compiles without Docker or an LLM:

```bash
cd labs/digital-worker
./mvnw -DskipTests package
```

The exercise tests intentionally start red and become green as the TODOs are completed. Answer implementations live in the slides, not in the starter source.

Serve the slides locally:

```bash
jwebserver -d docs -p 8000
```

Open <http://localhost:8000>. Press `S` for speaker notes.

- `Down` / `Up` moves within the current module.
- `Right` / `Left` jumps between Intro, Warmer, Part 1, Part 2, and Outro.
- `Esc` opens the two-dimensional overview for direct shortcuts.

After completing all five exercises, run the full local worker:

```bash
curl -fsS http://localhost:3000/api/health > /dev/null || \
  docker compose up -d grafana-lgtm
curl -fsS http://localhost:11434/api/tags > /dev/null || \
  docker compose up -d ollama
cd labs/digital-worker
./mvnw spring-boot:run
```

If the companion presentation's LGTM stack or Ollama is already running, the workshop reuses it. Redis remains available but is not required by the timed lab.

At the shell prompt, run `docker-compose-status`, `oom-incident`, or `latency-incident`.

After an incident command, wait about five seconds and validate all three signals:

```bash
./scripts/validate-telemetry.sh
```

The script queries the Prometheus, Loki, and Tempo data sources configured in Grafana and prints one `OK` line for metrics, logs, and traces.

## Repository Map

```text
docs/                       Reveal.js deck; answer slides follow exercises
labs/digital-worker/        one progressive Spring Boot lab
FACILITATOR_GUIDE.md        delivery, timing, and recovery notes
COMPANION_ALIGNMENT.md      shared claims and intentional workshop evolution
QUICK_REFERENCE.md          participant copy/paste reference
docker-compose.yml          same Ollama, Redis, and Grafana stack as the companion
```

The small `BoundedActionPlanner` intentionally makes GOAP mechanics visible. The application uses Embabel RC1's real `@Agent`, `@Action`, `@AchievesGoal`, action cost, and retry policy APIs while Spring AI remains pinned to 2.0.0.
