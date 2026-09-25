package com.garbigo.collection.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ setup for this service. This service only ever consumes queues
 * that other services (currently just auth-service) already own and
 * declare - it does not declare exchanges or bindings, only the queue
 * itself (a durable {@link Queue} declaration is idempotent: a no-op if it
 * already exists with the same properties), so
 * {@link com.garbigo.collection.messaging.UserCreatedEventListener} has
 * something to bind to on a fresh local environment too.
 *
 * <p>{@link JacksonJsonMessageConverter} is Spring AMQP 4's Jackson-3-based
 * converter - {@code Jackson2JsonMessageConverter} is deprecated for
 * removal as of Spring AMQP 4.0, in favor of this class. Spring Boot
 * auto-wires whatever {@link MessageConverter} bean is present into the
 * default listener container factory and {@code RabbitTemplate}, which is
 * what lets {@code @RabbitListener} methods declare a {@code JsonNode}
 * parameter instead of a raw byte array.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.user-created}")
    private String userCreatedQueueName;

    @Bean
    Queue userCreatedQueue() {
        return new Queue(userCreatedQueueName, true);
    }

    @Bean
    MessageConverter jacksonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}