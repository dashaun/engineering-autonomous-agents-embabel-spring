package com.example.digitalworker.worker;

import com.example.digitalworker.domain.DockerComposeStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DockerComposeService implements ComposeStatusReader {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String composeFile;

    public DockerComposeService(
            @Value("${workshop.compose-file:../../docker-compose.yml}") String composeFile) {
        this.composeFile = composeFile;
    }

    @Override
    public List<DockerComposeStatus> statuses() {
        try {
            String output = run("ps", "--all", "--format", "json");
            List<DockerComposeStatus> existing = parseStatuses(output);
            List<String> configured = run("config", "--services").lines()
                    .filter(service -> !service.isBlank())
                    .toList();
            return includeUncreatedServices(existing, configured);
        } catch (IOException exception) {
            throw new IllegalStateException("Docker is unavailable", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Docker status check interrupted", exception);
        }
    }

    private List<DockerComposeStatus> parseStatuses(String output) throws IOException {
        if (output.isBlank()) {
            return List.of();
        }
        JsonNode json = objectMapper.readTree(output);
        List<DockerComposeStatus> statuses = new ArrayList<>();
        if (json.isArray()) {
            json.forEach(node -> statuses.add(toStatus(node)));
        } else {
            statuses.add(toStatus(json));
        }
        return statuses;
    }

    private List<DockerComposeStatus> includeUncreatedServices(
            List<DockerComposeStatus> existing,
            List<String> configuredServices) {
        Map<String, DockerComposeStatus> byService = new LinkedHashMap<>();
        configuredServices.forEach(service -> byService.put(
                service,
                new DockerComposeStatus(service, "", service, "not created", "")));
        existing.forEach(status -> byService.put(status.service(), status));
        return new ArrayList<>(byService.values());
    }

    private String run(String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(
                "docker", "compose", "--file", composeFile));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Docker Compose command timed out");
        }
        String output = new String(
                process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.exitValue() != 0) {
            throw new IllegalStateException("Docker Compose failed: " + output);
        }
        return output;
    }

    private DockerComposeStatus toStatus(JsonNode node) {
        return new DockerComposeStatus(
                node.path("Name").asText(),
                node.path("Image").asText(),
                node.path("Service").asText(),
                node.path("Status").asText(node.path("State").asText()),
                node.path("Ports").toString());
    }
}
