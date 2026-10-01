package com.garbigo.collection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Backs @Async email sending (see MailService) with a small, bounded pool,
 * rather than Spring's default SimpleAsyncTaskExecutor, which spawns an
 * unbounded new thread per call - fine at low volume, a real risk under
 * load with no limit at all. Email sending is request-triggered but off
 * the request's own response path, and low-volume relative to the rest of
 * this service, so this doesn't need to be large.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mailTaskExecutor")
    Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mail-async-");
        executor.initialize();
        return executor;
    }
}