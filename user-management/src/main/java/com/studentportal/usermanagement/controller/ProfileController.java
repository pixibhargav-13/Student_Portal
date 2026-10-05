package com.studentportal.usermanagement.controller;

import com.studentportal.api.usermanagement.ProfileUserManagementApi;
import com.studentportal.api.usermanagement.model.UpdateProfileRequest;
import com.studentportal.api.usermanagement.model.User;
import com.studentportal.usermanagement.mapper.UserMapper;
import com.studentportal.usermanagement.security.CurrentUser;
import com.studentportal.usermanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints any logged-in user (admin, parent, tutor) can call for their own account. */
@RestController
@RequiredArgsConstructor
public class ProfileController implements ProfileUserManagementApi {

  private final UserService userService;
  private final UserMapper userMapper;

  @Override
  public ResponseEntity<User> getMyProfile() {
    return ResponseEntity.ok(userMapper.toUserResponse(userService.getById(CurrentUser.id())));
  }

  @Override
  public ResponseEntity<User> updateMyProfile(UpdateProfileRequest updateProfileRequest) {
    return ResponseEntity.ok(
        userMapper.toUserResponse(
            userService.updateProfile(CurrentUser.id(), updateProfileRequest)));
  }
}
