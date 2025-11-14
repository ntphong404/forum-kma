package com.forum.kma.authservice.controller;

import com.forum.kma.authservice.constant.AuthErrorCode;
import com.forum.kma.authservice.dto.response.SessionResponse;
import com.forum.kma.authservice.service.SessionService;
import com.forum.kma.authservice.model.User;
import com.forum.kma.common.dto.response.ApiResponse;
import com.forum.kma.common.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/auth/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public Mono<ApiResponse<List<SessionResponse>>> listSessions() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .cast(User.class)
                .flatMapMany(user -> sessionService.getAllSessionsTyped(user.getId()))
                .map(entry -> new SessionResponse(entry.getKey(), entry.getValue()))
                .collectList()
                .map(list -> ApiResponse.success("Fetched sessions", list))
                .switchIfEmpty(Mono.error(new AppException(AuthErrorCode.USER_NOT_EXISTED)));
    }

    @DeleteMapping("/{sessionId}")
    public Mono<ApiResponse<Void>> revokeSession(@PathVariable String sessionId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .cast(User.class)
                .flatMap(user -> sessionService.revokeSingleSession(user.getId(), sessionId)
                        .flatMap(deleted -> {
                            if (deleted) return Mono.just(ApiResponse.<Void>success("Session revoked", null));
                            return Mono.just(ApiResponse.<Void>error(404, "Session not found"));
                        }))
                .switchIfEmpty(Mono.error(new AppException(AuthErrorCode.USER_NOT_EXISTED)));
    }

    @DeleteMapping
    public Mono<ApiResponse<Void>> revokeAll() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .cast(User.class)
                .flatMap(user -> sessionService.revokeAllSessions(user.getId())
                        .flatMap(deleted -> Mono.just(ApiResponse.<Void>success("All sessions revoked", null))))
                .switchIfEmpty(Mono.error(new AppException(AuthErrorCode.USER_NOT_EXISTED)));
    }
}
