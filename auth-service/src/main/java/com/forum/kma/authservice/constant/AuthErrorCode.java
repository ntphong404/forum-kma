package com.forum.kma.authservice.constant;

import com.forum.kma.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum AuthErrorCode implements ErrorCode {
    USER_EXISTED(1001, "User already exists", HttpStatus.NOT_FOUND),
    ROLE_NOT_EXISTED(1002, "Role does not exist", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1003, "Invalid username or password", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "User does not exist" , HttpStatus.BAD_REQUEST ),
    UNAUTHORIZED(1004,"Invalid internal secret key." ,  HttpStatus.UNAUTHORIZED ),
    SESSION_REVOKED(1005,"Session has been revoked" ,HttpStatus.UNAUTHORIZED ),
    INVALID_TOKEN_TYPE(1006,"Invalid token type" , HttpStatus.UNAUTHORIZED ),
    SOMETHING_WRONG(1007,"SOMETHING_WRONG" , HttpStatus.BAD_REQUEST );

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    AuthErrorCode(int code, String message, HttpStatusCode statusCode) {
    this.code = code;
    this.message = message;
    this.statusCode = statusCode;
    }

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
