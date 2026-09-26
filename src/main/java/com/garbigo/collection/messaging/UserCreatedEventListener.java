package com.garbigo.collection.messaging;

import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.service.UserSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * JsonNode (not com.fasterxml.jackson) since Spring Boot 4 defaults to
 * Jackson 3. Field names below match auth-service's confirmed
 * GET /internal/users response (id/username/firstName/email/role/active/
 * archived) - assumed to also hold for this create event, not separately
 * confirmed. firstName/archived use .path(...) with a default since
 * they're the less certain of the fields for this specific event.
 */
@Component
@RequiredArgsConstructor
public class UserCreatedEventListener {

    private final UserSummaryService userSummaryService;

    @RabbitListener(queues = "${rabbitmq.queue.user-created}")
    public void onUserCreated(JsonNode event) {
        UserSummary userSummary = UserSummary.builder()
                .id(event.get("id").asString())
                .username(event.get("username").asString())
                .firstName(event.path("firstName").asString(null))
                .email(event.get("email").asString())
                .role(event.get("role").asString())
                .active(event.path("active").asBoolean(true))
                .archived(event.path("archived").asBoolean(false))
                .build();
        userSummaryService.upsert(userSummary);
    }
}