package com.example.digitalworker.domain;

public record IncidentRequest(IncidentType incidentType, String metrics, String logs) {

    public static IncidentRequest outOfMemory() {
        return new IncidentRequest(
                IncidentType.OUT_OF_MEMORY,
                "heap_usage=95%, gc_pause=5000ms, request_latency=500ms",
                "java.lang.OutOfMemoryError: Java heap space");
    }

    public static IncidentRequest highLatency() {
        return new IncidentRequest(
                IncidentType.HIGH_LATENCY,
                "p99_latency=5000ms, error_rate=0.5%",
                "WARN database query timeout; cache miss rate=60%");
    }
}
