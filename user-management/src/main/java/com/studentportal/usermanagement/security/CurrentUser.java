package com.studentportal.usermanagement.security;

import com.studentportal.common.exception.UnauthorizedException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Static access to the logged-in user for code that is not handed it explicitly. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CurrentUser {

  public static AuthUser get() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
      return user;
    }
    throw new UnauthorizedException(AuthErrorCodes.AUTH_REQUIRED, "Login required");
  }

  public static Long id() {
    return get().id();
  }
}
