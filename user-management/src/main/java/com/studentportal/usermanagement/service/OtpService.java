package com.studentportal.usermanagement.service;

import com.studentportal.common.exception.TooManyRequestsException;
import com.studentportal.common.exception.UnauthorizedException;
import com.studentportal.usermanagement.config.OtpProperties;
import com.studentportal.usermanagement.model.entity.OtpRequestEntity;
import com.studentportal.usermanagement.repository.OtpRequestRepository;
import com.studentportal.usermanagement.security.AuthErrorCodes;
import com.studentportal.usermanagement.sms.SmsSender;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates, sends and checks login OTPs. Verification is split into {@link #verify} and {@link
 * #consume} so a failed signup (e.g. role missing) does not burn a correct OTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

  private final OtpRequestRepository otpRequestRepository;
  private final PasswordEncoder passwordEncoder;
  private final SmsSender smsSender;
  private final OtpProperties otpProperties;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  @Builder
  public record SentOtp(int expiresInSeconds, int resendAfterSeconds, String devOtp) {}

  @Transactional
  public SentOtp send(String mobileNumber) {
    LocalDateTime now = LocalDateTime.now(clock);

    otpRequestRepository
        .findTopByMobileNumberOrderByCreatedAtDesc(mobileNumber)
        .ifPresent(
            last -> {
              long secondsSince = Duration.between(last.getCreatedAt(), now).toSeconds();
              long wait = otpProperties.getResendCooldownSeconds() - secondsSince;
              if (wait > 0) {
                throw new TooManyRequestsException(
                    "Please wait " + wait + " seconds before requesting another OTP");
              }
            });

    long sentLastHour =
        otpRequestRepository.countByMobileNumberAndCreatedAtAfter(mobileNumber, now.minusHours(1));
    if (sentLastHour >= otpProperties.getMaxPerHour()) {
      throw new TooManyRequestsException("Too many OTP requests. Please try again after an hour.");
    }

    // Only the newest OTP is valid
    otpRequestRepository.consumeAllActive(mobileNumber);

    String otp = generateOtp();
    otpRequestRepository.save(
        OtpRequestEntity.builder()
            .mobileNumber(mobileNumber)
            .otpHash(passwordEncoder.encode(otp))
            .expiresAt(now.plusSeconds(otpProperties.getValiditySeconds()))
            .createdAt(now)
            .build());

    smsSender.send(
        mobileNumber,
        otp
            + " is your Student Portal login OTP. Valid for "
            + otpProperties.getValiditySeconds() / 60
            + " minutes. Do not share it with anyone.");

    return SentOtp.builder()
        .expiresInSeconds(otpProperties.getValiditySeconds())
        .resendAfterSeconds(otpProperties.getResendCooldownSeconds())
        .devOtp(otpProperties.isDevMode() ? otp : null)
        .build();
  }

  /**
   * Checks the OTP without consuming it. Wrong attempts are counted (and kept even though an
   * exception is thrown) and the OTP is burnt after too many.
   *
   * @return id of the matched OTP, to pass to {@link #consume}
   */
  @Transactional(noRollbackFor = UnauthorizedException.class)
  public Long verify(String mobileNumber, String otp) {
    OtpRequestEntity request =
        otpRequestRepository
            .findTopByMobileNumberAndConsumedFalseOrderByCreatedAtDesc(mobileNumber)
            .filter(r -> !r.isExpired(LocalDateTime.now(clock)))
            .orElseThrow(
                () ->
                    new UnauthorizedException(
                        AuthErrorCodes.OTP_EXPIRED,
                        "OTP expired or not requested. Please request a new OTP."));

    if (!passwordEncoder.matches(otp, request.getOtpHash())) {
      request.setAttempts(request.getAttempts() + 1);
      int left = otpProperties.getMaxVerifyAttempts() - request.getAttempts();
      if (left <= 0) {
        request.setConsumed(true);
        otpRequestRepository.save(request);
        throw new UnauthorizedException(
            AuthErrorCodes.OTP_ATTEMPTS_EXCEEDED,
            "Too many wrong attempts. Please request a new OTP.");
      }
      otpRequestRepository.save(request);
      throw new UnauthorizedException(
          AuthErrorCodes.OTP_INVALID, "Incorrect OTP. " + left + " attempt(s) left.");
    }
    return request.getId();
  }

  @Transactional
  public void consume(Long otpRequestId) {
    otpRequestRepository
        .findById(otpRequestId)
        .ifPresent(
            r -> {
              r.setConsumed(true);
              otpRequestRepository.save(r);
            });
  }

  /** OTP rows are only needed for rate limiting within the last hour. */
  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void purgeOld() {
    int deleted = otpRequestRepository.deleteOlderThan(LocalDateTime.now(clock).minusDays(1));
    log.info("Purged {} old OTP requests", deleted);
  }

  private String generateOtp() {
    int bound = (int) Math.pow(10, otpProperties.getLength());
    return String.format("%0" + otpProperties.getLength() + "d", random.nextInt(bound));
  }
}
