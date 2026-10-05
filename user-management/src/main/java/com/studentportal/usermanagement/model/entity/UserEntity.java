package com.studentportal.usermanagement.model.entity;

import com.studentportal.common.audit.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Every person who can log in: admins (email + password), parents and tutors (mobile + OTP).
 * Role-specific data (tutor profile, child profiles) lives in its own module and points here by
 * {@code user_id}.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class UserEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String fullName;

  /** 10-digit number without country code. Null only for admins created with email. */
  @Column(unique = true)
  private String mobileNumber;

  /** Always stored lower-case. */
  @Column(unique = true)
  private String email;

  /** BCrypt hash. Only set for users who log in with email + password (admins). */
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private UserStatus status = UserStatus.ACTIVE;

  /** Why the admin blocked the account; shown to the user on login. */
  private String statusReason;

  @Enumerated(EnumType.STRING)
  private Gender gender;

  @Enumerated(EnumType.STRING)
  private Language preferredLanguage;

  private String city;
  private String pincode;
  private String profilePhotoUrl;

  @Builder.Default private Boolean mobileVerified = false;

  private LocalDateTime lastLoginAt;

  public boolean isActive() {
    return status == UserStatus.ACTIVE;
  }

  public boolean isProfileComplete() {
    return fullName != null && !fullName.isBlank();
  }
}
