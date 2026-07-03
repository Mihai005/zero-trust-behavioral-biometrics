package com.zerotrust.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void handleAuthResult(String jsonPayload) {
        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            String userId = root.get("userId").asString();
            int trustScore = root.get("trustScore").asInt();

            log.info("Routing new Trust Score for {}: {} to WebSocket", userId, trustScore);

            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/trust-score",
                    jsonPayload
            );

        } catch (Exception e) {
            log.error("Failed to route auth result from Python: {}", e.getMessage());
        }
    }
}
