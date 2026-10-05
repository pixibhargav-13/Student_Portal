package com.studentportal.usermanagement.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * One OTP sent to a mobile number. Only the hash is stored. Rows are also the source for rate
 * limiting (resend cooldown, max per hour) and are purged after a day.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "otp_requests")
public class OtpRequestEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String mobileNumber;

  @Column(nullable = false)
  private String otpHash;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @Builder.Default private Integer attempts = 0;

  @Builder.Default private Boolean consumed = false;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public boolean isExpired(LocalDateTime now) {
    return now.isAfter(expiresAt);
  }
}
