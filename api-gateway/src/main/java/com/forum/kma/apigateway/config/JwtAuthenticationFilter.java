package com.forum.kma.apigateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.kma.common.dto.response.ApiResponse;
import com.forum.kma.common.security.JwtClaims;
import com.forum.kma.common.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // Constructor để nhận cả JwtUtil và ObjectMapper
    public JwtAuthenticationFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    // Skip auth paths (e.g., login, refresh)
    private static final List<String> SKIP_PATHS = List.of(
            "/api/auth",
            "/api/users",
            "/api/roles"
    );

    private boolean shouldSkip(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        return SKIP_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> handleApiError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<?> apiResponse = ApiResponse.error(status.value(), message);

        DataBufferFactory bufferFactory = response.bufferFactory();

        try {
            // Chuyển đối tượng ApiResponse thành chuỗi JSON
            byte[] bytes = objectMapper.writeValueAsBytes(apiResponse);

            // Ghi JSON vào DataBuffer
            DataBuffer buffer = bufferFactory.wrap(bytes);

            // Ghi buffer vào response và hoàn tất
            return response.writeWith(Mono.just(buffer));

        } catch (JsonProcessingException e) {
            log.error("Error serializing ApiResponse: {}", e.getMessage());
            // Trả về phản hồi 401 mặc định nếu serialization thất bại
            return response.setComplete();
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (shouldSkip(exchange)) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        String token = authHeader.substring(7);

        try {
            JwtClaims claims = jwtUtil.validateToken(token);

            if (claims == null) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Ensure it's an access token
            if (!"access".equals(claims.type())) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String userId = claims.userId();
            String sessionId = claims.sid();

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-Session-Id", sessionId != null ? sessionId : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());

            String errorMessage = "JWT validation failed: " + e.getMessage();
            return handleApiError(exchange, errorMessage, HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public int getOrder() {
        // Execute early
        return -100;
    }
}
