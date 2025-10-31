package com.forum.kma.authservice.controller;

import com.forum.kma.authservice.dto.AuthResponse;
import com.forum.kma.authservice.dto.LoginRequest;
import com.forum.kma.authservice.dto.RefreshRequest;
import com.forum.kma.authservice.dto.RegisterRequest;
import com.forum.kma.authservice.service.AuthService;
import com.forum.kma.common.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Mono<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request, ServerWebExchange exchange) {

        return authService.register(request, exchange)
                .map(authResponse -> ApiResponse.success("Register success", authResponse));
    }

    @PostMapping("/login")
    public Mono<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request, ServerWebExchange exchange) {

        return authService.login(request, exchange)
                .map(authResponse -> ApiResponse.success("Login success", authResponse));
    }

    @PostMapping("/refresh")
    public Mono<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshRequest request, ServerWebExchange exchange) {

        return authService.refreshToken(request, exchange)
                .map(authResponse -> ApiResponse.success("Refreshed token", authResponse));
    }
}