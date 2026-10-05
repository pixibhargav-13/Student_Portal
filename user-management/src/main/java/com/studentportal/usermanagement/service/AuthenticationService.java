package com.studentportal.usermanagement.service;

import com.studentportal.api.usermanagement.model.AuthResponse;
import com.studentportal.api.usermanagement.model.SendOtpResponse;
import com.studentportal.api.usermanagement.model.VerifyOtpRequest;
import com.studentportal.common.exception.UnauthorizedException;
import com.studentportal.usermanagement.mapper.UserMapper;
import com.studentportal.usermanagement.model.entity.UserEntity;
import com.studentportal.usermanagement.repository.UserRepository;
import com.studentportal.usermanagement.security.AuthErrorCodes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Login flows. Deliberately not {@code @Transactional}: OTP checks and account creation run in
 * their own transactions so a wrong-OTP attempt is recorded even though the request fails.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final OtpService otpService;
  private final UserService userService;
  private final UserRepository userRepository;
  private final JwtTokenService jwtTokenService;
  private final UserMapper userMapper;

  public SendOtpResponse sendOtp(String mobileNumber) {
    OtpService.SentOtp sent = otpService.send(mobileNumber);
    return new SendOtpResponse()
        .message("OTP sent to " + mask(mobileNumber))
        .expiresInSeconds(sent.expiresInSeconds())
        .resendAfterSeconds(sent.resendAfterSeconds())
        .devOtp(sent.devOtp());
  }

  public AuthResponse verifyOtp(VerifyOtpRequest request) {
    Long otpId = otpService.verify(request.getMobileNumber(), request.getOtp());
    UserService.LoginResult result =
        userService.loginWithMobile(
            request.getMobileNumber(),
            userMapper.toEntityRole(request.getRole()),
            request.getFullName());
    otpService.consume(otpId);
    return tokens(result.user(), jwtTokenService.generateRefreshToken(result.user()))
        .newUser(result.newUser());
  }

  public AuthResponse signIn(String email, String password) {
    UserEntity user = userService.loginWithPassword(email, password);
    return tokens(user, jwtTokenService.generateRefreshToken(user)).newUser(false);
  }

  /** Issues a new access token; the refresh token is returned unchanged (same as ERP). */
  public AuthResponse refresh(String refreshToken) {
    UserEntity user;
    try {
      Claims claims = jwtTokenService.parse(refreshToken);
      if (!JwtTokenService.TYPE_REFRESH.equals(claims.get(JwtTokenService.CLAIM_TYPE))) {
        throw invalidRefreshToken();
      }
      user =
          userRepository
              .findById(Long.valueOf(claims.getSubject()))
              .orElseThrow(this::invalidRefreshToken);
    } catch (JwtException | IllegalArgumentException e) {
      throw invalidRefreshToken();
    }
    userService.assertCanLogin(user);
    return tokens(user, refreshToken).newUser(false);
  }

  private AuthResponse tokens(UserEntity user, String refreshToken) {
    return new AuthResponse()
        .accessToken(jwtTokenService.generateAccessToken(user))
        .refreshToken(refreshToken)
        .accessTokenExpiresIn(jwtTokenService.accessTokenValiditySeconds())
        .user(userMapper.toUserResponse(user));
  }

  private UnauthorizedException invalidRefreshToken() {
    return new UnauthorizedException(
        AuthErrorCodes.TOKEN_INVALID, "Invalid or expired refresh token. Please log in again.");
  }

  private static String mask(String mobileNumber) {
    return "XXXXXX" + mobileNumber.substring(mobileNumber.length() - 4);
  }
}
