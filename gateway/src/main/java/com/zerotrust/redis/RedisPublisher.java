package com.zerotrust.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisPublisher {
    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic biometricStreamTopic;

    public void publishKeystrokes(String jsonPayload) {
        log.info("Publishing keystrokes to Redis...");
        redisTemplate.convertAndSend(biometricStreamTopic.getTopic(), jsonPayload);
    }
}
