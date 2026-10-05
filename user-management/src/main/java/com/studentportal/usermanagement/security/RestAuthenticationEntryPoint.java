package com.studentportal.usermanagement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentportal.api.usermanagement.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** JSON 401/403 bodies for requests rejected by the security filter chain (before controllers). */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

  private static final Map<String, String> MESSAGES =
      Map.of(
          AuthErrorCodes.AUTH_REQUIRED, "Login required",
          AuthErrorCodes.TOKEN_EXPIRED, "Access token expired, refresh it",
          AuthErrorCodes.TOKEN_INVALID, "Invalid access token",
          AuthErrorCodes.ACCOUNT_BLOCKED, "Your account is not active. Please contact support.");

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    Object attribute = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
    String code = attribute != null ? attribute.toString() : AuthErrorCodes.AUTH_REQUIRED;
    write(response, HttpServletResponse.SC_UNAUTHORIZED, code, MESSAGES.get(code));
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    write(
        response,
        HttpServletResponse.SC_FORBIDDEN,
        AuthErrorCodes.ACCESS_DENIED,
        "You do not have permission to do this");
  }

  private void write(HttpServletResponse response, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(), new ErrorResponse().code(code).message(message));
  }
}
