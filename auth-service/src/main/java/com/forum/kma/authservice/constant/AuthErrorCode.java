package com.forum.kma.authservice.constant;

import com.forum.kma.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum AuthErrorCode implements ErrorCode {
    USER_EXISTED(1001, "User already exists", HttpStatus.CONFLICT),
    ROLE_NOT_EXISTED(1002, "Role does not exist", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1003, "Invalid username or password", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "User does not exist" , HttpStatus.BAD_REQUEST ),
    UNAUTHORIZED(1004,"Invalid internal secret key." ,  HttpStatus.UNAUTHORIZED ),
    SESSION_REVOKED(1005,"Session has been revoked" ,HttpStatus.UNAUTHORIZED ),
    INVALID_TOKEN_TYPE(1006,"Invalid token type" , HttpStatus.UNAUTHORIZED ),
    SOMETHING_WRONG(1007,"Something went wrong" , HttpStatus.BAD_REQUEST ),
    USER_BANNED(1008, "User is banned", HttpStatus.FORBIDDEN),
    TWO_FACTOR_REQUIRED(1009, "Two factor authentication required", HttpStatus.ACCEPTED),
    PASSWORD_NOT_MATCH(1010, "Password does not match" , HttpStatus.BAD_REQUEST ),
    INVALID_TOKEN(1011,"Invalid token" , HttpStatus.UNAUTHORIZED ),
    OTP_CODE_INVALID(1012,"OTP code is invalid" , HttpStatus.BAD_REQUEST ),
    REDIS_SAVE_FAILED(1013,"Failed to save data to Redis" ,HttpStatus.BAD_REQUEST ),
    DATABASE_SAVE_FAILED(1014, "An error occurred while saving data to the database", HttpStatus.INTERNAL_SERVER_ERROR ),
    USER_ALREADY_ACTIVE(1015,"User already active" , HttpStatus.BAD_REQUEST ),
    OTP_ALREADY_SENT(1016, "OTP already sent", HttpStatus.TOO_MANY_REQUESTS);


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
