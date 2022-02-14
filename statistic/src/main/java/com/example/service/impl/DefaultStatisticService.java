package com.example.service.impl;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Function;

import com.example.controller.vm.UserVM;
import com.example.controller.vm.UsersStatisticVM;
import com.example.repository.MessageRepository;
import com.example.repository.UserRepository;
import com.example.service.StatisticService;
import com.example.service.gitter.dto.MessageResponse;
import com.example.service.impl.utils.MessageMapper;
import com.example.service.impl.utils.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * @author Xiaobo Bi (869384236@qq.com)
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.repository",
        "com.example.repository.impl",
        "com.example.service.impl"
})
@EnableReactiveMongoRepositories(basePackages = {
        "com.example.repository",
        "com.example.repository.impl"
})
@Slf4j
public class DefaultStatisticService implements StatisticService {
    private static final UserVM EMPTY_USER = new UserVM("", "");

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    @Autowired
    public DefaultStatisticService(UserRepository userRepository,
            MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(DefaultStatisticService.class, args);
    }

    @Bean
    @Override
    public Function<Flux<MessageResponse>, Flux<UsersStatisticVM>> updateStatistic() {
        return messagesFlux -> messagesFlux.map(MessageMapper::toDomainUnit)
                .doOnRequest(count -> log.info("Requested " + count + " messages"))
                .transform(messageRepository::saveAll)
                .doOnError(error -> log.error(error.getMessage(), error))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(500)))
                .onBackpressureLatest()
                .concatMap(e -> this.doGetUserStatistic(), 1)
                .onErrorContinue((t, e) -> {});
    }

    private Mono<UsersStatisticVM> doGetUserStatistic() {
        Mono<UserVM> topActiveUserMono = userRepository.findMostActive()
                .map(UserMapper::toViewModelUnits)
                .defaultIfEmpty(EMPTY_USER);

        Mono<UserVM> topMentionedUserMono = userRepository.findMostPopular()
                .map(UserMapper::toViewModelUnits)
                .defaultIfEmpty(EMPTY_USER);

        return Mono.zip(topActiveUserMono, topMentionedUserMono, UsersStatisticVM::new)
                .timeout(Duration.ofSeconds(2));
    }
}
