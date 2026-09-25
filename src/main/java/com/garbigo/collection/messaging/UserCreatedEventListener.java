package com.garbigo.collection.messaging;

import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.service.UserSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Consumes auth-service's existing {@code user-created-queue} and upserts
 * into the local {@link UserSummary} cache on each message.
 *
 * <p>The parameter type is {@code JsonNode} rather than a concrete event
 * class, deliberately: {@link com.garbigo.collection.config.RabbitMQConfig}
 * configures a {@code JacksonJsonMessageConverter}, which (with Spring
 * AMQP's default inferred type precedence) deserializes to whatever type
 * the listener method declares - so this works regardless of what
 * {@code __TypeId__} header auth-service's publisher sets, without this
 * service needing auth-service's event class on its classpath.
 *
 * <p>Note the import: Spring Boot 4 defaults to Jackson 3, whose classes
 * live under {@code tools.jackson.*} rather than {@code com.fasterxml.jackson.*}
 * (the exception being {@code jackson-annotations}, which stays on the old
 * package for backward compatibility) - {@code com.fasterxml.jackson.databind.JsonNode}
 * won't resolve here since plain jackson-databind 2.x isn't on the classpath.
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

    @RabbitListener(queues = "${rabbitmq.queue.user-created}")
    public void onUserCreated(JsonNode event) {
        UserSummary userSummary = UserSummary.builder()
                .id(event.get("id").asString())
                .email(event.get("email").asString())
                .displayUsername(event.get("displayUsername").asString())
                .role(event.get("role").asString())
                .active(event.path("active").asBoolean(true))
                .build();
        userSummaryService.upsert(userSummary);
    }
}