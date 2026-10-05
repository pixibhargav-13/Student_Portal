package com.studentportal.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the hand-written OpenAPI specs so Swagger UI shows them exactly as written. Add one inner
 * controller per spec and register its URL under {@code springdoc.swagger-ui.urls}.
 */
@Configuration
public class OpenApiConfig {

  private static String readSpec(String fileName) throws IOException {
    ClassPathResource cpr = new ClassPathResource(fileName);
    return new String(FileCopyUtils.copyToByteArray(cpr.getInputStream()), StandardCharsets.UTF_8);
  }

  @RestController
  @RequestMapping("/doc/v3/user-management")
  public static class UserManagementDocumentationController {
    @GetMapping
    public String getOpenApiYaml() throws IOException {
      return readSpec("user-management.yaml");
    }
  }
}
