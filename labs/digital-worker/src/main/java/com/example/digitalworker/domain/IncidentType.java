package com.example.digitalworker.domain;

public enum IncidentType {
    OUT_OF_MEMORY("OutOfMemory"),
    HIGH_LATENCY("High latency");

    private final String displayName;

    IncidentType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
