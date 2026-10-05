package com.studentportal.usermanagement.config;

import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.Delimiter;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.cors")
public class CorsProperties {
  private List<CorsMapping> mappings = List.of();

  @Data
  public static class CorsMapping {
    private String path;

    @Delimiter(",")
    private List<String> allowedOrigins;

    private List<String> allowedMethods;
    private Boolean allowCredentials;
  }
}
