package com.studentportal.usermanagement.security;

import com.studentportal.usermanagement.model.entity.UserEntity;
import com.studentportal.usermanagement.repository.UserRepository;
import com.studentportal.usermanagement.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the bearer token, loads the user and puts an {@link AuthUser} in the security context. The
 * user is re-read on every request so blocking an account takes effect immediately. When a token is
 * rejected the reason is left as a request attribute for {@link RestAuthenticationEntryPoint}.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  public static final String AUTH_ERROR_ATTRIBUTE = "studentportal.auth.error";

  private final JwtTokenService jwtTokenService;
  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      Claims claims = jwtTokenService.parse(authHeader.substring(7));
      if (!JwtTokenService.TYPE_ACCESS.equals(claims.get(JwtTokenService.CLAIM_TYPE))) {
        // A refresh token must never work as an access token
        request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCodes.TOKEN_INVALID);
      } else {
        Optional<UserEntity> user = userRepository.findById(Long.valueOf(claims.getSubject()));
        if (user.isEmpty()) {
          request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCodes.TOKEN_INVALID);
        } else if (!user.get().isActive()) {
          request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCodes.ACCOUNT_BLOCKED);
        } else {
          AuthUser principal = AuthUser.from(user.get());
          var authToken =
              new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (ExpiredJwtException e) {
      request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCodes.TOKEN_EXPIRED);
    } catch (JwtException | IllegalArgumentException e) {
      request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCodes.TOKEN_INVALID);
    }

    filterChain.doFilter(request, response);
  }
}
