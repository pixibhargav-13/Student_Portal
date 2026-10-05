package com.studentportal.usermanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Validities are in milliseconds, same as Parmeshwari ERP. */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
  /** Base64-encoded HMAC key, at least 256 bits. */
  private String secretKey;

  private long accessTokenValidity;
  private long refreshTokenValidity;
}
