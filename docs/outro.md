<!-- .slide: data-background-color="#6db33f" -->

## Your Production Blueprint

```text
Typed input
  → observable Spring action
  → domain policy
  → constrained model reasoning
  → explicit goal
  → bounded, auditable execution
```

Start with one valuable goal and the smallest safe action surface.

Notes:
Part 2, minute 86-88.
Use Right Arrow from Part 2 to reach this final horizontal column. This is the organizational handoff, not another implementation section.

Likely questions:
- Q: Do we begin with a general-purpose enterprise agent? A: No. Begin with one bounded goal, a small reviewed action surface, and measurable value.
- Q: Which part should be deterministic? A: Eligibility, policy, authorization, bounds, and completion. Use model reasoning where ambiguity adds value.

---

## Before You Ship

- every risky capability has a guard;
- every action declares what it needs and produces;
- every plan has a hard bound;
- every external failure has an explicit outcome;
- every production change retains human approval;
- every execution records actions, failures, and result;
- integration tests prove the end-to-end goal.

Notes:
Part 2, minute 88-89.

Likely questions:
- Q: Is this the complete production checklist? A: No. Add security review, privacy, model evaluation, capacity planning, durable recovery, and incident response for the worker itself.
- Q: Does observability include chain-of-thought? A: No. Capture action selection, inputs at approved sensitivity, outputs, failures, timing, and goal status.

---

## Stop Building Chatbots

# Start Building Workers

DaShaun Carter | Spring Developer Advocate

[DaShaun.com](https://dashaun.com)

> What goal could a typed worker safely pursue in your system tomorrow?

Notes:
Part 2, minute 89-90.
End on the question and wait. The best closing discussion is a participant naming one safe business goal and its first two typed actions.

Likely questions:
- Q: Where should I learn more? A: Use the Embabel documentation and companion repository linked from DaShaun.com, then adapt this lab's domain/action/goal pattern.
- Q: What is the central takeaway? A: Stop treating the model call as the architecture. Build disciplined workers whose capabilities and boundaries live in your Java system.
