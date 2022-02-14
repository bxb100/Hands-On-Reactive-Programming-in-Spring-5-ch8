package com.example.controller;

import java.util.Arrays;
import java.util.function.Function;

import com.example.controller.vm.MessageVM;
import com.example.controller.vm.UsersStatisticVM;
import com.example.service.gitter.dto.MessageResponse;
import com.example.service.impl.utils.MessageMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;

@RestController
@RequestMapping("/api/v1/info")
@SpringBootApplication(scanBasePackages = "com.example.controller")
public class InfoResource {

    private final Sinks.Many<UsersStatisticVM> statisticStream = Sinks.many().replay().latest();
    private final Sinks.Many<MessageVM> messagesStream = Sinks.many().replay().limit(50);

    public static void main(String[] args) {
        SpringApplication.run(InfoResource.class, args);
    }

    @Bean
    public Function<Tuple2<Flux<MessageResponse>, Flux<UsersStatisticVM>>, Mono<Void>> listen() {
        return tuple2 -> {
            tuple2.getT1().map(MessageMapper::toViewModelUnit)
                    .subscribe(messagesStream::tryEmitNext);
            tuple2.getT2().subscribe(statisticStream::tryEmitNext);
            return Mono.empty();
        };
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<?> stream() {
        return Flux.merge(messagesStream.asFlux(), statisticStream.asFlux());
    }
}