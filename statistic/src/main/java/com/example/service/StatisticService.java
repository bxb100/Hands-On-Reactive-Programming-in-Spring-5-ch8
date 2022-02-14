package com.example.service;

import java.util.function.Function;

import com.example.controller.vm.UsersStatisticVM;
import com.example.service.gitter.dto.MessageResponse;
import reactor.core.publisher.Flux;

/**
 * @author Xiaobo Bi (869384236@qq.com)
 */
public interface StatisticService {

    Function<Flux<MessageResponse>, Flux<UsersStatisticVM>> updateStatistic();
}
