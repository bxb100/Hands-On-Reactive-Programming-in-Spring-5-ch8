package com.example.service.impl;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;

import com.example.service.ChatService;
import com.example.service.gitter.GitterProperties;
import com.example.service.gitter.GitterUriBuilder;
import com.example.service.gitter.dto.MessageResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.function.context.PollableBean;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@SpringBootApplication(scanBasePackages = "com.example.service.impl")
@EnableConfigurationProperties(GitterProperties.class)
@Slf4j
public class GitterService implements ChatService<MessageResponse> {

    private final WebClient webClient;
    private final GitterProperties gitterProperties;

    @Autowired
    public GitterService(WebClient.Builder builder, GitterProperties gitterProperties) {
        this.webClient = builder.defaultHeader("Authorization", "Bearer " + gitterProperties.getAuth().getToken()).build();
        this.gitterProperties = gitterProperties;
    }

    public static void main(String... args) {

        SpringApplication.run(GitterService.class, args);
    }

    @Bean
    public Supplier<Flux<MessageResponse>> getMessagesStream() {
        return () -> webClient.get()
                .uri(GitterUriBuilder.from(gitterProperties.getStream())
                        .build()
                        .toUri())
                .retrieve()
                .onStatus(HttpStatus::is2xxSuccessful, r -> {log.warn("getMessagesStream 请求一次"); return Mono.empty();})
                .bodyToFlux(MessageResponse.class)
                .doOnError(error -> log.error("getMessagesStream Error: " + error.getMessage()))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(500)));
    }

    @SneakyThrows
    @Bean
    public Supplier<Flux<MessageResponse>> getLatestMessages() {
        return () -> webClient.get()
                .uri(GitterUriBuilder.from(gitterProperties.getApi())
                        .build()
                        .toUri())
                .retrieve()
                .onStatus(HttpStatus::is2xxSuccessful, r -> {log.warn("getLatestMessages 请求一次"); return Mono.empty();})
                .bodyToFlux(MessageResponse.class)
                .timeout(Duration.ofSeconds(2))
                .doOnError(error -> log.error("getLatestMessages Error: " + error.getMessage()))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(500)));
    }
}
