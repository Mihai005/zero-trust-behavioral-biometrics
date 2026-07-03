package com.zerotrust.security;

import com.zerotrust.service.JwtService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;

    private static final int TRUST_THRESHOLD = 70;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            try {
                String authHeader = accessor.getFirstNativeHeader("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    String username = jwtService.extractUsername(token);

                    if (username != null) {
                        var userDetails = userDetailsService.loadUserByUsername(username);

                        if (jwtService.isTokenValid(token, userDetails)) {
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()
                            );
                            accessor.setUser(auth);
                            log.debug("WebSocket authenticated successfully for user: {}", username);
                        } else {
                            log.warn("WebSocket connection rejected: Invalid JWT for user {}", username);
                            return null;
                        }
                    } else {
                        log.warn("WebSocket connection rejected: Username could not be extracted.");
                        return null;
                    }
                } else {
                    log.warn("WebSocket connection rejected: Missing or malformed Authorization header");
                    return null;
                }
            } catch (Exception e) {
                log.error("WebSocket JWT Authentication failed: {}", e.getMessage());
                return null;
            }
        }

        else if (StompCommand.SEND.equals(command)) {
            Principal user = accessor.getUser();

            if (user == null) {
                log.warn("STOMP FRAME DROPPED: Unauthenticated SEND attempt.");
                throw new AccessDeniedException("Zero-Trust Policy Violation: Unauthenticated session.");
            }

            String username = user.getName();
            String redisKey = "trust_score:" + username;
            String scoreStr = redisTemplate.opsForValue().get(redisKey);

            if (scoreStr != null) {
                try {
                    int currentScore = Integer.parseInt(scoreStr);
                    if (currentScore < TRUST_THRESHOLD) {
                        log.warn("STOMP FRAME DROPPED: User {} failed Zero-Trust check (Score: {}).", username, currentScore);
                        throw new AccessDeniedException("Zero-Trust Policy Violation: Biometric score compromised.");
                    }
                } catch (NumberFormatException e) {
                    log.error("Invalid biometric state in Redis for user {}: {}", username, scoreStr);
                    throw new AccessDeniedException("Zero-Trust Policy Violation: Invalid biometric state.");
                }
            }
        }

        return message;
    }
}
