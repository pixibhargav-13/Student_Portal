package com.studentportal.usermanagement.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserManagementConfig {

  /** Injected instead of calling now() directly so time-based rules (OTP expiry) are testable. */
  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
