package com.forum.kma.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum CommonErrorCode implements ErrorCode {
  UNCATEGORIZED_EXCEPTION(500, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
  USER_EXISTED(409, "User already exists", HttpStatus.CONFLICT),
  USER_NOT_EXISTED(404, "User not found", HttpStatus.NOT_FOUND),
  UNAUTHENTICATED(401, "Unauthenticated", HttpStatus.UNAUTHORIZED),
  UNAUTHORIZED(403, "Unauthorized", HttpStatus.FORBIDDEN),
  ROLE_NOT_EXISTED(404, "Role not existed", HttpStatus.NOT_FOUND),
  INTERNAL_ERROR(400, "Internal error" , HttpStatus.INTERNAL_SERVER_ERROR ),;

  private final int code;
  private final String message;
  private final HttpStatusCode statusCode;

  CommonErrorCode(int code, String message, HttpStatusCode statusCode) {
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
