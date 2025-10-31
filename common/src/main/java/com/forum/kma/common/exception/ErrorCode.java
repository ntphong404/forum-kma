package com.forum.kma.common.exception;

import org.springframework.http.HttpStatusCode;

public interface ErrorCode {
    int getCode();

    String getMessage();

    HttpStatusCode getStatusCode();
}