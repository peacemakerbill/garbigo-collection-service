package com.garbigo.collection.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Code-only alternative to purging queues via the RabbitMQ UI. Off by default. */
@Component
@ConditionalOnProperty(name = "rabbitmq.purge-queues-on-startup", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class QueuePurgeRunner implements CommandLineRunner {

    private final AmqpAdmin amqpAdmin;

    @Value("${rabbitmq.queue.user-created}")
    private String userCreatedQueueName;

    @Override
    public void run(String... args) {
        int purged = amqpAdmin.purgeQueue(userCreatedQueueName);
        log.warn("Purged {} message(s) from '{}'. Set PURGE_QUEUES_ON_STARTUP back to false.",
                purged, userCreatedQueueName);
    }
}