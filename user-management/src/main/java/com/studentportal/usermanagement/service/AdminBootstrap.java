package com.studentportal.usermanagement.service;

import com.studentportal.usermanagement.config.BootstrapAdminProperties;
import com.studentportal.usermanagement.model.entity.UserEntity;
import com.studentportal.usermanagement.model.entity.UserRole;
import com.studentportal.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin from {@code app.bootstrap-admin.*} when the database has none, so a fresh
 * environment is usable without manual SQL. Does nothing once any admin exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

  private final BootstrapAdminProperties properties;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(ApplicationArguments args) {
    if (userRepository.existsByRole(UserRole.ADMIN)) {
      return;
    }
    if (UserService.trimToNull(properties.getEmail()) == null
        || UserService.trimToNull(properties.getPassword()) == null) {
      log.warn("No admin exists and app.bootstrap-admin.email/password are not set");
      return;
    }
    userRepository.save(
        UserEntity.builder()
            .fullName(properties.getFullName())
            .email(UserService.normalizeEmail(properties.getEmail()))
            .password(passwordEncoder.encode(properties.getPassword()))
            .role(UserRole.ADMIN)
            .build());
    log.info("Created bootstrap admin {}", properties.getEmail());
  }
}
