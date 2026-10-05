package com.studentportal.usermanagement.controller;

import com.studentportal.api.usermanagement.UserManagementUserManagementApi;
import com.studentportal.api.usermanagement.model.AdminUpdateUserRequest;
import com.studentportal.api.usermanagement.model.CreateUserRequest;
import com.studentportal.api.usermanagement.model.PaginatedResultUser;
import com.studentportal.api.usermanagement.model.UpdateUserStatusRequest;
import com.studentportal.api.usermanagement.model.User;
import com.studentportal.api.usermanagement.model.UserRole;
import com.studentportal.api.usermanagement.model.UserStatus;
import com.studentportal.api.usermanagement.model.UserSummary;
import com.studentportal.usermanagement.mapper.UserMapper;
import com.studentportal.usermanagement.security.CurrentUser;
import com.studentportal.usermanagement.security.roles.Admin;
import com.studentportal.usermanagement.service.UserService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Admin panel user management. Every endpoint is admin-only. */
@RestController
@RequiredArgsConstructor
@Admin
public class UserManagementController implements UserManagementUserManagementApi {

  private final UserService userService;
  private final UserMapper userMapper;

  @Override
  public ResponseEntity<PaginatedResultUser> getAllUsers(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> search,
      Optional<UserRole> role,
      Optional<UserStatus> status,
      Optional<String> sortBy,
      Optional<String> direction) {
    return ResponseEntity.ok(
        userService.search(
            page,
            size,
            search,
            role.map(userMapper::toEntityRole),
            status.map(userMapper::toEntityStatus),
            sortBy,
            direction));
  }

  @Override
  public ResponseEntity<User> createUser(CreateUserRequest createUserRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(userMapper.toUserResponse(userService.create(createUserRequest)));
  }

  @Override
  public ResponseEntity<UserSummary> getUserSummary() {
    return ResponseEntity.ok(userService.summary());
  }

  @Override
  public ResponseEntity<User> getUserById(Long id) {
    return ResponseEntity.ok(userMapper.toUserResponse(userService.getById(id)));
  }

  @Override
  public ResponseEntity<User> updateUser(Long id, AdminUpdateUserRequest adminUpdateUserRequest) {
    return ResponseEntity.ok(
        userMapper.toUserResponse(userService.update(id, adminUpdateUserRequest)));
  }

  @Override
  public ResponseEntity<User> updateUserStatus(
      Long id, UpdateUserStatusRequest updateUserStatusRequest) {
    return ResponseEntity.ok(
        userMapper.toUserResponse(
            userService.updateStatus(
                CurrentUser.id(),
                id,
                userMapper.toEntityStatus(updateUserStatusRequest.getStatus()),
                updateUserStatusRequest.getReason())));
  }

  @Override
  public ResponseEntity<Void> deleteUser(Long id) {
    userService.delete(CurrentUser.id(), id);
    return ResponseEntity.noContent().build();
  }
}
