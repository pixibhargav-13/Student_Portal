package com.studentportal.exceptionhandler;

import com.studentportal.api.usermanagement.model.ErrorResponse;
import com.studentportal.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Every error leaves the API as {@link ErrorResponse} {@code {code, message, detail}}. Modules
 * throw subclasses of {@link ApiException} from the common module instead of adding handlers here.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(
      HttpServletRequest request, ApiException ex) {
    if (ex.getStatus().is5xxServerError()) {
      log.error("{} {} failed", request.getMethod(), request.getRequestURI(), ex);
    } else {
      log.info(
          "{} {} -> {} {}",
          request.getMethod(),
          request.getRequestURI(),
          ex.getStatus().value(),
          ex.getMessage());
    }
    return error(ex.getStatus(), ex.getCode(), ex.getMessage(), null);
  }

  /** Request body failed the OpenAPI constraints (pattern, required, min/max...). */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, Object> fields = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
    return error(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_FAILED",
        "Please correct the highlighted fields",
        fields);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    Map<String, Object> fields = new LinkedHashMap<>();
    ex.getConstraintViolations()
        .forEach(v -> fields.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request", fields);
  }

  /** Malformed JSON or an unknown enum value. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", rootMessage(ex), null);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return error(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_FAILED",
        "Invalid value '" + ex.getValue() + "' for '" + ex.getName() + "'",
        null);
  }

  /** Thrown by @Admin/@Parent/@Tutor checks on controller methods. */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    return error(
        HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to do this", null);
  }

  /** Last line of defence for unique constraints hit by concurrent requests. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(
      HttpServletRequest request, DataIntegrityViolationException ex) {
    log.warn("Data integrity violation on {}: {}", request.getRequestURI(), rootMessage(ex));
    return error(
        HttpStatus.CONFLICT, "CONFLICT", "This record conflicts with an existing one", null);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
    return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "No endpoint " + ex.getResourcePath(), null);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex.getMessage(), null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAll(HttpServletRequest request, Exception ex) {
    log.error("{} {} failed", request.getMethod(), request.getRequestURI(), ex);
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Something went wrong. Please try again.",
        null);
  }

  private static ResponseEntity<ErrorResponse> error(
      HttpStatus status, String code, String message, Map<String, Object> detail) {
    ErrorResponse body = new ErrorResponse().code(code).message(message);
    if (detail != null && !detail.isEmpty()) {
      body.setDetail(detail);
    }
    return ResponseEntity.status(status).body(body);
  }

  private static String rootMessage(Throwable ex) {
    Throwable root = ex;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    return root.getMessage();
  }
}
