package com.garbigo.collection.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.service.UserSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes auth-service's existing {@code user-created-queue} and upserts
 * into the local {@link UserSummary} cache on each message.
 *
 * <p>The parameter type is {@code JsonNode} rather than a concrete event
 * class, deliberately: {@link com.garbigo.collection.config.RabbitMQConfig}
 * configures a {@code Jackson2JsonMessageConverter}, which (with Spring
 * AMQP's default inferred type precedence) deserializes to whatever type
 * the listener method declares - so this works regardless of what
 * {@code __TypeId__} header auth-service's publisher sets, without this
 * service needing auth-service's event class on its classpath.
 *
 * <p>TODO: the field names read below (id/email/displayUsername/role/
 * active) are a guess matching {@link UserSummary}'s own fields - confirm
 * against auth-service's actual published event shape. Consider a
 * dedicated UserCreatedEvent DTO instead of parsing a raw JsonNode once
 * that shape is confirmed, so a missing/renamed field fails at
 * deserialization time instead of throwing a NullPointerException here.
 */
@Component
@RequiredArgsConstructor
public class UserCreatedEventListener {

    private final UserSummaryService userSummaryService;

    @RabbitListener(queues = "${garbigo.messaging.user-created-queue}")
    public void onUserCreated(JsonNode event) {
        UserSummary userSummary = UserSummary.builder()
                .id(event.get("id").asText())
                .email(event.get("email").asText())
                .displayUsername(event.get("displayUsername").asText())
                .role(event.get("role").asText())
                .active(event.path("active").asBoolean(true))
                .build();
        userSummaryService.upsert(userSummary);
    }
}
