package com.studentportal.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base for exceptions that map straight to an HTTP status in the global handler. */
@Getter
public abstract class ApiException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  protected ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}
