package com.forum.kma.common.exception;

import com.forum.kma.common.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<Void>> handlingRuntimeException(Exception exception) {
        log.error("Exception: ", exception);
        ApiResponse<Void> apiResponse = new ApiResponse<>();

        apiResponse.setCode(CommonErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        apiResponse.setMessage(CommonErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<Void> apiResponse = new ApiResponse<>();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> handlingResourceNotFound (NoResourceFoundException exception) {
        log.error("Exception: ", exception);
        ApiResponse<Void> apiResponse = new ApiResponse<>();

        apiResponse.setCode(exception.getStatusCode().value());
        apiResponse.setMessage(exception.getMessage());

        return ResponseEntity.status(exception.getStatusCode()).body(apiResponse);
    }
}
