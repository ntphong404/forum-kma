package com.forum.kma.postservice.exception;

import com.forum.kma.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {
    RESOURCE_NOT_FOUND(2404,"Resource not found", HttpStatus.NOT_FOUND)
    ;

    int code;
    String message;
    HttpStatusCode statusCode;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
