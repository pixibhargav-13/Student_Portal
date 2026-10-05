package com.studentportal.usermanagement.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
  /** Ant-style paths reachable without a token. */
  private List<String> publicAccess = new ArrayList<>();
}
