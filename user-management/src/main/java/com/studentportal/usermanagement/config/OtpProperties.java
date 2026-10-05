package com.studentportal.usermanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "otp")
public class OtpProperties {
  private int length = 6;
  private int validitySeconds = 300;
  private int resendCooldownSeconds = 30;
  private int maxPerHour = 5;
  private int maxVerifyAttempts = 5;

  /**
   * Local/dev only: return the OTP in the API response so frontend devs can log in without an SMS
   * provider. Must be false in production.
   */
  private boolean devMode = false;
}
