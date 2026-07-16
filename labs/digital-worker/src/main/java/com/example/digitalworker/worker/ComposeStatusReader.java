package com.example.digitalworker.worker;

import com.example.digitalworker.domain.DockerComposeStatus;

import java.util.List;

@FunctionalInterface
public interface ComposeStatusReader {
    List<DockerComposeStatus> statuses();
}
