## Stop Just Calling APIs

```java
String answer = chatClient.prompt()
    .user("Fix production")
    .call()
    .content();
```

One prompt has no durable state, no business boundary, no plan, and no proof of completion.

> Production AI needs to **work toward a goal** inside a domain.

Notes:
Minute 3-6.
Ask who has shipped prompt/response code. The issue is not ChatClient; the issue is pretending one call is a workflow.

Likely questions:
- Q: Is prompt/response code bad? A: No. It is the correct primitive for many features; it is insufficient when work requires state, policy, multiple capabilities, and a completion condition.
- Q: Is this just tool calling? A: Tool calling selects a model-requested function. Planning reasons over typed actions, conditions, state, and goals across multiple steps.

---

## What You Will Build

```text
IncidentRequest
      ↓
OBSERVE  Docker Compose → ServiceObservation
      ↓
ORIENT   Java runbook → RunbookAssessment
      ↓
DECIDE   Spring AI → IncidentResponseReport
      ↓
ACT      Embabel goal → IncidentWorkflowReport
```

A Digital Worker that can choose a safe path, call real Spring services, repair a failed plan, and stop.

Notes:
Minute 6. Name the four OODA labels, but do not explain them yet. The important promise is that every arrow becomes code participants can test.

Likely questions:
- Q: Will this worker restart production services? A: No. Its action surface is deliberately read-only and produces an approval-required report.
- Q: Is Docker Compose the business domain? A: No. Compose is the live observation source; incident response and runbook policy are the domain.

---

## The Required Stack

| COMPONENT | VERSION | ROLE |
|---|---:|---|
| Spring Boot | `4.1.0` | application runtime |
| Spring AI | `2.0.0` | typed model interaction |
| Embabel | `1.0.0-RC1` | actions, goals, planning model |
| Java | `21+` | RC1 artifact requirement |
| Ollama | local | Qwen 2.5 Coder 1.5B |

The versions are fixed. The domain is ours.

Notes:
Minute 6-8.
Spring Boot 4.1 supports Java 17, but the Embabel RC1 artifact is compiled for Java 21. Say this now to avoid setup churn.

Likely questions:
- Q: Why use RC1? A: It is the version contract shared with the companion presentation. The workshop favors consistency over silently upgrading APIs.
- Q: Does the model require a cloud key? A: No. Ollama runs the prepackaged local Qwen model from Docker Compose.
- Q: Why is Spring AI pinned separately? A: The workshop explicitly demonstrates Spring AI 2.0.0 while using Embabel RC1 annotations.

---

## Two 90-Minute Parts

### Part 1 — From Prompting to Planning

OODA, GOAP, DICE, typed state, and the action graph

### Part 2 — Building the Digital Worker

Spring Beans as actions, goals, bounded execution, failure, and plan repair

**One domain. One project. One working outcome.**

Notes:
Minute 8-10.
Participants work in labs/digital-worker for both halves.

Likely questions:
- Q: Is Part 1 lecture-only? A: No. It includes the domain-policy and action-graph exercises.
- Q: Are there separate lab projects? A: No. One progressive project avoids branch and setup churn.
- Q: Where are the answers? A: Each exercise is immediately followed by an answer slide in the same vertical stack.
