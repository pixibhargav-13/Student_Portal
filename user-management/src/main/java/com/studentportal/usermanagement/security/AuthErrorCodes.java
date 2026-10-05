package com.studentportal.usermanagement.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Stable {@code code} values in error responses. The frontend switches on these (refresh the token
 * on TOKEN_EXPIRED, force logout on ACCOUNT_BLOCKED), so do not rename them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthErrorCodes {
  public static final String AUTH_REQUIRED = "AUTH_REQUIRED";
  public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
  public static final String TOKEN_INVALID = "TOKEN_INVALID";
  public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
  public static final String OTP_INVALID = "OTP_INVALID";
  public static final String OTP_EXPIRED = "OTP_EXPIRED";
  public static final String OTP_ATTEMPTS_EXCEEDED = "OTP_ATTEMPTS_EXCEEDED";
  public static final String ROLE_REQUIRED = "ROLE_REQUIRED";
  public static final String ACCOUNT_BLOCKED = "ACCOUNT_BLOCKED";
  public static final String ACCOUNT_DELETED = "ACCOUNT_DELETED";
  public static final String ACCESS_DENIED = "ACCESS_DENIED";
}
