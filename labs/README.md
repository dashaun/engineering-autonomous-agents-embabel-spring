# Hands-On Lab

Both halves use one progressive project: [`digital-worker`](digital-worker/README.md).

The starter compiles but intentionally omits every exercise answer. `TODO Exercise` comments mark the participant work. Each exercise is followed by its answer slide, so late arrivals can copy the answer and catch up without changing branches.

```bash
cd digital-worker
./mvnw -DskipTests package
```

The focused tests require no Docker daemon and no model. They become green progressively as participants complete the lab. The live run uses the root `docker-compose.yml`, and the telemetry script validates its LGTM stack.
