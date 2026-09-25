package com.garbigo.collection.messaging;

import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.service.UserSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** JsonNode (not com.fasterxml.jackson) since Spring Boot 4 defaults to Jackson 3. */
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