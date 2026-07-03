package com.zerotrust.controller;

import com.zerotrust.dto.LoginRequestDTO;
import com.zerotrust.dto.LoginResponseDTO;
import com.zerotrust.dto.RegisterRequestDTO;
import com.zerotrust.service.AuthService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<@NonNull LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        var token = authService.login(loginRequest);
        return ResponseEntity.ok(token);
    }
}
