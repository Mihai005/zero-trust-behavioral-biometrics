package com.zerotrust.service;

import com.zerotrust.dto.LoginRequestDTO;
import com.zerotrust.dto.LoginResponseDTO;
import com.zerotrust.dto.RegisterRequestDTO;
import com.zerotrust.entity.UserEntity;
import com.zerotrust.exception.InvalidInputException;
import com.zerotrust.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final StringRedisTemplate stringRedisTemplate;

    public void register(RegisterRequestDTO registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new InvalidInputException("Username already exists");
        }

        String passwordHash = passwordEncoder.encode(registerRequest.getPassword());
        UserEntity newUser = UserEntity.builder()
                .username(registerRequest.getUsername())
                .passwordHash(passwordHash)
                .build();

        userRepository.save(newUser);
    }

    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        var user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new InvalidInputException("Username doesn't exist"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new InvalidInputException("Invalid password");
        }

        String username = user.getUsername();

        stringRedisTemplate.delete("trust_score:" + username);

        stringRedisTemplate.opsForValue().set("biometric_reset:" + username, "1");

        stringRedisTemplate.opsForValue().set("profile_in_progress:" + username, "1", Duration.ofMinutes(30));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);
        return new LoginResponseDTO(token);
    }
}
