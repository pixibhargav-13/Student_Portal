package com.studentportal.usermanagement.controller;

import com.studentportal.api.usermanagement.AuthenticationUserManagementApi;
import com.studentportal.api.usermanagement.model.AuthResponse;
import com.studentportal.api.usermanagement.model.RefreshTokenRequest;
import com.studentportal.api.usermanagement.model.SendOtpRequest;
import com.studentportal.api.usermanagement.model.SendOtpResponse;
import com.studentportal.api.usermanagement.model.SignInRequest;
import com.studentportal.api.usermanagement.model.VerifyOtpRequest;
import com.studentportal.usermanagement.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthenticationController implements AuthenticationUserManagementApi {

  private final AuthenticationService authenticationService;

  @Override
  public ResponseEntity<SendOtpResponse> sendOtp(SendOtpRequest sendOtpRequest) {
    return ResponseEntity.ok(authenticationService.sendOtp(sendOtpRequest.getMobileNumber()));
  }

  @Override
  public ResponseEntity<AuthResponse> verifyOtp(VerifyOtpRequest verifyOtpRequest) {
    return ResponseEntity.ok(authenticationService.verifyOtp(verifyOtpRequest));
  }

  @Override
  public ResponseEntity<AuthResponse> signIn(SignInRequest signInRequest) {
    return ResponseEntity.ok(
        authenticationService.signIn(signInRequest.getEmail(), signInRequest.getPassword()));
  }

  @Override
  public ResponseEntity<AuthResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
    return ResponseEntity.ok(authenticationService.refresh(refreshTokenRequest.getRefreshToken()));
  }
}
