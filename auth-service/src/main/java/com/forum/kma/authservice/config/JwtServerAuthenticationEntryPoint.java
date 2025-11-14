package com.forum.kma.authservice.config;

import org.springframework.security.core.AuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.kma.common.dto.response.ApiResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component("customJwtAuthenticationEntryPoint")
public class JwtServerAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
  @Override
  public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
    ApiResponse<?> apiResponse = ApiResponse.builder()
        .code(401)
        .message("Bạn chưa đăng nhập hoặc token không hợp lệ")
        .build();
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String body = objectMapper.writeValueAsString(apiResponse);
      DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
      DataBuffer buffer = bufferFactory.wrap(body.getBytes(StandardCharsets.UTF_8));

      // If response already committed, avoid writing headers/body again
      if (exchange.getResponse().isCommitted()) {
        return exchange.getResponse().setComplete();
      }

      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (Exception e) {
      return exchange.getResponse().setComplete();
    }
  }
}
