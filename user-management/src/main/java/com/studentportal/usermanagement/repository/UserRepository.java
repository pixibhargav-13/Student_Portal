package com.studentportal.usermanagement.repository;

import com.studentportal.common.repository.CoreRepository;
import com.studentportal.usermanagement.model.entity.UserEntity;
import com.studentportal.usermanagement.model.entity.UserRole;
import com.studentportal.usermanagement.model.entity.UserStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CoreRepository<UserEntity, Long> {

  Optional<UserEntity> findByMobileNumber(String mobileNumber);

  Optional<UserEntity> findByEmail(String email);

  boolean existsByMobileNumber(String mobileNumber);

  boolean existsByEmail(String email);

  boolean existsByMobileNumberAndIdNot(String mobileNumber, Long id);

  boolean existsByEmailAndIdNot(String email, Long id);

  boolean existsByRole(UserRole role);

  long countByStatusNot(UserStatus status);

  long countByRoleAndStatusNot(UserRole role, UserStatus status);

  long countByStatus(UserStatus status);

  long countByStatusNotAndCreatedAtAfter(UserStatus status, LocalDateTime after);
}
