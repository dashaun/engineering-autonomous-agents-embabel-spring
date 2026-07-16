package com.example.digitalworker.config;

import java.time.Duration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class WorkerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RestClient.Builder restClientBuilder(
            @Value("${workshop.ollama.request-timeout:300s}") Duration timeout) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
