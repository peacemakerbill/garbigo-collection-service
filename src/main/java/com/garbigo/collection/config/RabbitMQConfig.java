package com.garbigo.collection.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
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
 * <p>The {@link Jackson2JsonMessageConverter} bean here is what lets
 * {@code @RabbitListener} methods declare a concrete/JsonNode parameter
 * type instead of a raw byte array - Spring Boot auto-wires whatever
 * {@link MessageConverter} bean is present into the default listener
 * container factory and {@code RabbitTemplate}.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${garbigo.messaging.user-created-queue}")
    private String userCreatedQueueName;

    @Bean
    public Queue userCreatedQueue() {
        return new Queue(userCreatedQueueName, true);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
