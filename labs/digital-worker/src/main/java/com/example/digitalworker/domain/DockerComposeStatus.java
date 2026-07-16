package com.example.digitalworker.domain;

public record DockerComposeStatus(
        String name,
        String image,
        String service,
        String status,
        String ports) {
}
