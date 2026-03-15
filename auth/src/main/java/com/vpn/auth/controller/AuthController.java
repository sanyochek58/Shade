package com.vpn.auth.controller;

import com.vpn.auth.dto.request.LoginRequest;
import com.vpn.auth.dto.request.RefreshRequest;
import com.vpn.auth.dto.request.RegisterRequest;
import com.vpn.auth.dto.response.AuthResponse;
import com.vpn.auth.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest){
        log.info("Регистрация: {}", registerRequest.getEmail());
        AuthResponse authResponse = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        log.info("Логин: {}", loginRequest.getEmail());
        AuthResponse authResponse = authService.login(loginRequest);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest refreshRequest){
        AuthResponse authResponse = authService.refresh(refreshRequest);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("X-User-Id") Long userId){
        authService.logout(userId);
        return ResponseEntity.ok(Map.of("message", "Успещный выход"));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    ){
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "email", email,
                "role", role
        ));
    }
}
