package com.zerotrust.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ZeroTrustFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    private static final int TRUST_THRESHOLD = 70;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        final String username = authentication.getName();

        try {
            String redisKey = "trust_score:" + username;
            String scoreString = redisTemplate.opsForValue().get(redisKey);

            if (scoreString == null) {
                log.warn("No biometric trust score for user {} — checking profiling state", username);

                String profileKey = "profile_in_progress:" + username;
                String profiling = redisTemplate.opsForValue().get(profileKey);

                String path = request.getRequestURI();
                String method = request.getMethod();

                if (profiling != null && "GET".equalsIgnoreCase(method)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                log.warn("BLOCKING REQUEST: User {} has no biometric trust score and is not allowed: {} {}", username, method, path);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Access Denied: Missing biometric trust score. Please complete profiling.\", \"profiling\": " + (profiling != null ? "true" : "false") + "}");
                return;
            }

            int currentScore = Integer.parseInt(scoreString);

            if (currentScore < TRUST_THRESHOLD) {
                log.warn("BLOCKING REQUEST: User {} trust score is {}%", username, currentScore);

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Access Denied: Biometric trust score too low. Please re-authenticate.\"}");
                return;
            }

            filterChain.doFilter(request, response);

        } catch (NumberFormatException e) {
            log.warn("BLOCKING REQUEST: User {} has invalid biometric trust score", username);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Access Denied: Invalid biometric trust score. Please re-authenticate.\"}");
        } catch (Exception e) {
            log.error("Redis lookup failed during Zero-Trust enforcement", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") ||
                path.startsWith("/ws/") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui");
    }
}
