package com.studentportal.usermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studentportal.common.exception.TooManyRequestsException;
import com.studentportal.common.exception.UnauthorizedException;
import com.studentportal.usermanagement.config.OtpProperties;
import com.studentportal.usermanagement.model.entity.OtpRequestEntity;
import com.studentportal.usermanagement.repository.OtpRequestRepository;
import com.studentportal.usermanagement.sms.SmsSender;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

  private static final String MOBILE = "9876543210";
  private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-05T06:00:00Z"), ZONE);
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

  @Mock private OtpRequestRepository repository;
  @Mock private SmsSender smsSender;

  private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
  private final OtpProperties properties = new OtpProperties();
  private OtpService otpService;

  @BeforeEach
  void setUp() {
    properties.setDevMode(true);
    otpService = new OtpService(repository, encoder, smsSender, properties, CLOCK);
  }

  @Test
  void sendStoresHashSendsSmsAndReturnsDevOtp() {
    when(repository.findTopByMobileNumberOrderByCreatedAtDesc(MOBILE)).thenReturn(Optional.empty());

    OtpService.SentOtp sent = otpService.send(MOBILE);

    ArgumentCaptor<OtpRequestEntity> saved = ArgumentCaptor.forClass(OtpRequestEntity.class);
    verify(repository).save(saved.capture());
    assertThat(sent.devOtp()).matches("\\d{6}");
    assertThat(saved.getValue().getOtpHash()).isNotEqualTo(sent.devOtp());
    assertThat(encoder.matches(sent.devOtp(), saved.getValue().getOtpHash())).isTrue();
    assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plusSeconds(300));
    verify(repository).consumeAllActive(MOBILE);
    verify(smsSender).send(eq(MOBILE), any());
  }

  @Test
  void devOtpIsHiddenWhenDevModeIsOff() {
    properties.setDevMode(false);
    when(repository.findTopByMobileNumberOrderByCreatedAtDesc(MOBILE)).thenReturn(Optional.empty());

    assertThat(otpService.send(MOBILE).devOtp()).isNull();
  }

  @Test
  void resendWithinCooldownIsRejected() {
    when(repository.findTopByMobileNumberOrderByCreatedAtDesc(MOBILE))
        .thenReturn(Optional.of(otp("hash", NOW.minusSeconds(10))));

    assertThatThrownBy(() -> otpService.send(MOBILE))
        .isInstanceOf(TooManyRequestsException.class)
        .hasMessageContaining("20 seconds");
    verify(smsSender, never()).send(anyString(), anyString());
  }

  @Test
  void hourlyLimitIsEnforced() {
    when(repository.findTopByMobileNumberOrderByCreatedAtDesc(MOBILE))
        .thenReturn(Optional.of(otp("hash", NOW.minusMinutes(5))));
    when(repository.countByMobileNumberAndCreatedAtAfter(MOBILE, NOW.minusHours(1))).thenReturn(5L);

    assertThatThrownBy(() -> otpService.send(MOBILE)).isInstanceOf(TooManyRequestsException.class);
  }

  @Test
  void correctOtpIsVerifiedButNotConsumed() {
    OtpRequestEntity request = otp(encoder.encode("123456"), NOW.minusMinutes(1));
    when(repository.findTopByMobileNumberAndConsumedFalseOrderByCreatedAtDesc(MOBILE))
        .thenReturn(Optional.of(request));

    assertThat(otpService.verify(MOBILE, "123456")).isEqualTo(request.getId());
    assertThat(request.getConsumed()).isFalse();
  }

  @Test
  void wrongOtpCountsAttemptsAndBurnsOtpAtTheLimit() {
    OtpRequestEntity request = otp(encoder.encode("123456"), NOW.minusMinutes(1));
    request.setAttempts(3);
    when(repository.findTopByMobileNumberAndConsumedFalseOrderByCreatedAtDesc(MOBILE))
        .thenReturn(Optional.of(request));

    assertThatThrownBy(() -> otpService.verify(MOBILE, "000000"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("1 attempt(s) left");
    assertThat(request.getAttempts()).isEqualTo(4);

    assertThatThrownBy(() -> otpService.verify(MOBILE, "000000"))
        .isInstanceOf(UnauthorizedException.class)
        .extracting("code")
        .isEqualTo("OTP_ATTEMPTS_EXCEEDED");
    assertThat(request.getConsumed()).isTrue();
  }

  @Test
  void expiredOtpIsRejected() {
    OtpRequestEntity request = otp(encoder.encode("123456"), NOW.minusMinutes(10));
    when(repository.findTopByMobileNumberAndConsumedFalseOrderByCreatedAtDesc(MOBILE))
        .thenReturn(Optional.of(request));

    assertThatThrownBy(() -> otpService.verify(MOBILE, "123456"))
        .isInstanceOf(UnauthorizedException.class)
        .extracting("code")
        .isEqualTo("OTP_EXPIRED");
  }

  private static OtpRequestEntity otp(String hash, LocalDateTime createdAt) {
    return OtpRequestEntity.builder()
        .id(1L)
        .mobileNumber(MOBILE)
        .otpHash(hash)
        .createdAt(createdAt)
        .expiresAt(createdAt.plusMinutes(5))
        .build();
  }
}
