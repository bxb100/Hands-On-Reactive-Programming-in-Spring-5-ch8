package com.example.service;

import java.util.function.Supplier;

import reactor.core.publisher.Flux;

public interface ChatService<T> {

	Supplier<Flux<T>> getMessagesStream();

	Supplier<Flux<T>> getLatestMessages();
}
