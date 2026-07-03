package com.zerotrust.controller;

import com.zerotrust.dto.KeystrokeBatchPayload;
import com.zerotrust.redis.RedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import tools.jackson.databind.ObjectMapper;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class KeystrokeController {

    private final RedisPublisher redisPublisher;
    private final ObjectMapper objectMapper;

    @MessageMapping("/stream-biometrics")
    public void handleKeystrokes(@Payload KeystrokeBatchPayload payload, Principal principal) {

        if (principal == null || principal.getName() == null) {
            log.warn("Unauthenticated WebSocket message received. Dropping payload.");
            return;
        }

        try {
            payload.setUserId(principal.getName());

            String jsonString = objectMapper.writeValueAsString(payload);
            redisPublisher.publishKeystrokes(jsonString);

        } catch (Exception e) {
            log.error("Failed to process incoming keystroke batch: {}", e.getMessage());
        }
    }
}
