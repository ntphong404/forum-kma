package com.forum.kma.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.kma.common.dto.response.ApiResponse;
import com.forum.kma.common.exception.AppException;
import com.forum.kma.common.exception.CommonErrorCode; // Giả định bạn có CommonErrorCode
import com.forum.kma.common.exception.ErrorCode;
import io.netty.handler.codec.DecoderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.Exceptions;

import java.lang.reflect.UndeclaredThrowableException;

@Slf4j
@Component
@Order(-1) // Quan trọng: Đảm bảo nó chạy trước DefaultErrorWebExceptionHandler
public class ApiGatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    // Sử dụng constructor injection
    public ApiGatewayExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public @NonNull Mono<Void> handle(@NonNull ServerWebExchange exchange,@NonNull Throwable ex) {
        // 1. Trích xuất lỗi gốc
        Throwable originalException = extractOriginalException(ex);

        // 2. Chuẩn bị ApiResponse và Status Code dựa trên lỗi gốc
        ApiResponse<Void> apiResponse = new ApiResponse<>();
        HttpStatus httpStatus;

        if (originalException instanceof AppException appException) {
            ErrorCode errorCode = appException.getErrorCode();
            apiResponse.setCode(errorCode.getCode());
            apiResponse.setMessage(errorCode.getMessage());
            httpStatus = HttpStatus.valueOf(errorCode.getStatusCode().value());
        } else if (originalException instanceof ResponseStatusException rse) {
            // Xử lý lỗi HTTP chuẩn của Spring (ví dụ: 404 Not Found từ Gateway)
            apiResponse.setCode(rse.getStatusCode().value());
            apiResponse.setMessage(rse.getReason() != null ? rse.getReason() : "Gateway Error");
            httpStatus = HttpStatus.valueOf(rse.getStatusCode().value());
        } else {
            // Xử lý các lỗi Runtime không xác định hoặc lỗi mạng/kết nối (Default 500/503)
            apiResponse.setCode(CommonErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
            apiResponse.setMessage("Internal Server Error: " + originalException.getMessage());
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            // Ghi log lỗi hệ thống này để debug
            log.info("Internal Server Error: {}", originalException.getMessage());
        }

        // 3. Ghi Response
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBufferFactory bufferFactory = response.bufferFactory();
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(apiResponse);
            return response.writeWith(Mono.just(bufferFactory.wrap(bytes)));
        } catch (JsonProcessingException e) {
            // Nếu không thể serialize lỗi, trả về lỗi 500 thô
            return Mono.error(new IllegalStateException("Error writing response body", e));
        }
    }

    /**
     * Phương thức đệ quy để trích xuất lỗi gốc, loại bỏ các lớp bao bọc của Reactor/Spring.
     */
    private Throwable extractOriginalException(Throwable ex) {
        // Sử dụng Exceptions.unwrap() của Reactor để loại bỏ wrapper Mono/Flux cơ bản

        Throwable current = Exceptions.unwrap(ex);

        // Tiếp tục duyệt qua chuỗi lỗi để tìm lỗi sâu nhất
        while (current.getCause() != null &&
                current.getCause() != current &&
                (current instanceof UndeclaredThrowableException ||
                        current instanceof DecoderException || // Lỗi mạng phổ biến trong Netty
                        current instanceof ResponseStatusException)) {

            current = current.getCause();
        }
        return current;
    }
}