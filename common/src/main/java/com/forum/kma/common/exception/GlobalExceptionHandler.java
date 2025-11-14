package com.forum.kma.common.exception;

import com.forum.kma.common.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // Generic exception
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleGenericException(Exception ex, ServerWebExchange exchange) {
        // If response already committed, we can't modify headers or body and we should avoid noisy stacktraces
        if (exchange != null && exchange.getResponse().isCommitted()) {
            log.trace("Response already committed, skipping exception response write ({}: {})", ex.getClass().getSimpleName(), ex.getMessage());
            return Mono.empty();
        }

        log.error("Unhandled exception: {}", ex.getMessage());

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(CommonErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        response.setMessage(CommonErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return Mono.just(ResponseEntity.badRequest().body(response));
    }

    // Custom AppException
    @ExceptionHandler(AppException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleAppException(AppException ex, ServerWebExchange exchange) {
        if (exchange != null && exchange.getResponse().isCommitted()) {
            log.trace("Response already committed, skipping AppException response write ({}: {})", ex.getClass().getSimpleName(), ex.getMessage());
            return Mono.empty();
        }

        log.error("AppException: {}", ex.getMessage());

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ex.getErrorCode().getCode());
        response.setMessage(ex.getErrorCode().getMessage());

        return Mono.just(ResponseEntity
                .status(ex.getErrorCode().getStatusCode())
                .body(response));
    }

    // Resource not found
    @ExceptionHandler(NoResourceFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleNoResourceFoundException(NoResourceFoundException ex, org.springframework.web.server.ServerWebExchange exchange) {
        if (exchange != null && exchange.getResponse().isCommitted()) {
            log.trace("Response already committed, skipping NoResourceFoundException response write ({}: {})", ex.getClass().getSimpleName(), ex.getMessage());
            return Mono.empty();
        }

        log.error("NoResourceFoundException: {}", ex.getMessage());

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ex.getStatusCode().value());
        response.setMessage(ex.getMessage());

        return Mono.just(ResponseEntity
                .status(ex.getStatusCode())
                .body(response));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleValidationException(WebExchangeBindException ex) {
        log.error("Validation error: {}", ex.getMessage());

        // Gom tất cả message lỗi lại thành 1 chuỗi
        String errorMessages = ex.getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(CommonErrorCode.INVALID_ARGUMENT.getCode());
        response.setMessage(errorMessages);

        return Mono.just(ResponseEntity
                .badRequest()
                .body(response));
    }
}