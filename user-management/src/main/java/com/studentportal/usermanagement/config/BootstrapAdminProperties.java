package com.studentportal.usermanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** First admin account, created on startup only when no admin exists yet. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public class BootstrapAdminProperties {
  private String email;
  private String password;
  private String fullName = "Super Admin";
}
