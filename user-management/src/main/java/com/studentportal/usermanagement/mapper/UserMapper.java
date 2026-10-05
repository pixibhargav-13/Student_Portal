package com.studentportal.usermanagement.mapper;

import com.studentportal.api.usermanagement.model.User;
import com.studentportal.usermanagement.model.entity.UserEntity;
import com.studentportal.usermanagement.model.entity.UserRole;
import com.studentportal.usermanagement.model.entity.UserStatus;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.mapstruct.Mapper;

/** Entity and API enums share constant names, so MapStruct maps them by name. */
@Mapper(componentModel = "spring")
public interface UserMapper {

  User toUserResponse(UserEntity user);

  List<User> toUserResponseList(List<UserEntity> users);

  UserRole toEntityRole(com.studentportal.api.usermanagement.model.UserRole role);

  UserRole toEntityRole(com.studentportal.api.usermanagement.model.SignupRole role);

  UserStatus toEntityStatus(com.studentportal.api.usermanagement.model.UserStatus status);

  UserStatus toEntityStatus(com.studentportal.api.usermanagement.model.ManageableUserStatus status);

  com.studentportal.usermanagement.model.entity.Gender toEntityGender(
      com.studentportal.api.usermanagement.model.Gender gender);

  com.studentportal.usermanagement.model.entity.Language toEntityLanguage(
      com.studentportal.api.usermanagement.model.Language language);

  default OffsetDateTime toOffsetDateTime(LocalDateTime value) {
    return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
  }
}
