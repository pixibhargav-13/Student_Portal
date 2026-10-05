package com.studentportal.usermanagement.service;

import com.studentportal.api.usermanagement.model.AdminUpdateUserRequest;
import com.studentportal.api.usermanagement.model.CreateUserRequest;
import com.studentportal.api.usermanagement.model.PaginatedResultUser;
import com.studentportal.api.usermanagement.model.UpdateProfileRequest;
import com.studentportal.api.usermanagement.model.User;
import com.studentportal.api.usermanagement.model.UserSummary;
import com.studentportal.common.exception.BadRequestException;
import com.studentportal.common.exception.ConflictException;
import com.studentportal.common.exception.ForbiddenException;
import com.studentportal.common.exception.NotFoundException;
import com.studentportal.common.exception.UnauthorizedException;
import com.studentportal.common.util.PaginationUtils;
import com.studentportal.usermanagement.mapper.UserMapper;
import com.studentportal.usermanagement.model.entity.UserEntity;
import com.studentportal.usermanagement.model.entity.UserRole;
import com.studentportal.usermanagement.model.entity.UserStatus;
import com.studentportal.usermanagement.repository.UserRepository;
import com.studentportal.usermanagement.security.AuthErrorCodes;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account lifecycle: OTP signup/login, password login, profile edits and admin operations. */
@Service
@RequiredArgsConstructor
public class UserService {

  private static final Set<String> SORT_FIELDS =
      Set.of("createdAt", "lastUpdatedAt", "fullName", "lastLoginAt");

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final Clock clock;

  public record LoginResult(UserEntity user, boolean newUser) {}

  // ---------- login ----------

  /**
   * Logs in the user that owns {@code mobileNumber}, creating a PARENT/TUTOR account on first
   * login. The caller has already verified the OTP.
   */
  @Transactional
  public LoginResult loginWithMobile(String mobileNumber, UserRole signupRole, String fullName) {
    Optional<UserEntity> existing = userRepository.findByMobileNumber(mobileNumber);
    if (existing.isPresent()) {
      UserEntity user = existing.get();
      assertCanLogin(user);
      user.setMobileVerified(true);
      user.setLastLoginAt(LocalDateTime.now(clock));
      return new LoginResult(userRepository.save(user), false);
    }

    if (signupRole == null) {
      throw new BadRequestException(
          AuthErrorCodes.ROLE_REQUIRED,
          "No account for this number yet. Choose PARENT or TUTOR to sign up.");
    }
    if (signupRole == UserRole.ADMIN) {
      throw new BadRequestException("Admin accounts cannot be created by signup");
    }
    UserEntity user =
        UserEntity.builder()
            .mobileNumber(mobileNumber)
            .fullName(trimToNull(fullName))
            .role(signupRole)
            .mobileVerified(true)
            .lastLoginAt(LocalDateTime.now(clock))
            .build();
    return new LoginResult(userRepository.save(user), true);
  }

  @Transactional
  public UserEntity loginWithPassword(String email, String password) {
    UserEntity user =
        userRepository
            .findByEmail(normalizeEmail(email))
            .filter(
                u -> u.getPassword() != null && passwordEncoder.matches(password, u.getPassword()))
            .orElseThrow(
                () ->
                    new UnauthorizedException(
                        AuthErrorCodes.INVALID_CREDENTIALS, "Invalid email or password"));
    assertCanLogin(user);
    user.setLastLoginAt(LocalDateTime.now(clock));
    return userRepository.save(user);
  }

  /** Blocked/deleted users get a 403 with a code the frontend can show a proper message for. */
  public void assertCanLogin(UserEntity user) {
    if (user.getStatus() == UserStatus.BLOCKED) {
      String reason = user.getStatusReason() != null ? " Reason: " + user.getStatusReason() : "";
      throw new ForbiddenException(
          AuthErrorCodes.ACCOUNT_BLOCKED,
          "Your account has been blocked." + reason + " Please contact support.");
    }
    if (user.getStatus() == UserStatus.DELETED) {
      throw new ForbiddenException(
          AuthErrorCodes.ACCOUNT_DELETED, "This account has been deleted. Please contact support.");
    }
  }

  // ---------- own profile ----------

  public UserEntity getById(Long id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
  }

  @Transactional
  public UserEntity updateProfile(Long userId, UpdateProfileRequest request) {
    UserEntity user = getById(userId);
    String email = normalizeEmail(request.getEmail());
    assertEmailFree(email, userId);

    user.setFullName(request.getFullName().trim());
    user.setEmail(email);
    user.setGender(userMapper.toEntityGender(request.getGender()));
    user.setPreferredLanguage(userMapper.toEntityLanguage(request.getPreferredLanguage()));
    user.setCity(trimToNull(request.getCity()));
    user.setPincode(trimToNull(request.getPincode()));
    user.setProfilePhotoUrl(trimToNull(request.getProfilePhotoUrl()));
    return userRepository.save(user);
  }

  // ---------- admin ----------

  public PaginatedResultUser search(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> search,
      Optional<UserRole> role,
      Optional<UserStatus> status,
      Optional<String> sortBy,
      Optional<String> direction) {
    PageRequest pageable =
        PaginationUtils.getPageRequest(page, size, direction, sortBy, SORT_FIELDS);

    Specification<UserEntity> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          search
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .ifPresent(
                  s -> {
                    String like = "%" + s.toLowerCase(Locale.ROOT) + "%";
                    predicates.add(
                        cb.or(
                            cb.like(cb.lower(root.get("fullName")), like),
                            cb.like(root.get("mobileNumber"), like),
                            cb.like(root.get("email"), like)));
                  });
          role.ifPresent(r -> predicates.add(cb.equal(root.get("role"), r)));
          // Deleted users are hidden unless explicitly asked for
          predicates.add(
              status
                  .map(s -> cb.equal(root.get("status"), s))
                  .orElseGet(() -> cb.notEqual(root.get("status"), UserStatus.DELETED)));
          return cb.and(predicates.toArray(Predicate[]::new));
        };

    Page<UserEntity> result = userRepository.findAll(spec, pageable);
    List<User> users = userMapper.toUserResponseList(result.getContent());
    return new PaginatedResultUser()
        .data(users)
        .empty(result.isEmpty())
        .first(result.isFirst())
        .last(result.isLast())
        .totalPages(result.getTotalPages())
        .totalElements(result.getTotalElements())
        .size(users.size())
        .page(result.getNumber());
  }

  @Transactional
  public UserEntity create(CreateUserRequest request) {
    UserRole role = userMapper.toEntityRole(request.getRole());
    String email = normalizeEmail(request.getEmail());
    String mobile = trimToNull(request.getMobileNumber());

    if (role == UserRole.ADMIN && (email == null || request.getPassword() == null)) {
      throw new BadRequestException("Admins need an email and a password");
    }
    if (role != UserRole.ADMIN && mobile == null) {
      throw new BadRequestException("Parents and tutors need a mobile number");
    }
    assertMobileFree(mobile, null);
    assertEmailFree(email, null);

    UserEntity user =
        UserEntity.builder()
            .fullName(request.getFullName().trim())
            .role(role)
            .mobileNumber(mobile)
            .email(email)
            .password(
                request.getPassword() != null
                    ? passwordEncoder.encode(request.getPassword())
                    : null)
            .gender(userMapper.toEntityGender(request.getGender()))
            .city(trimToNull(request.getCity()))
            .pincode(trimToNull(request.getPincode()))
            .build();
    return userRepository.save(user);
  }

  @Transactional
  public UserEntity update(Long id, AdminUpdateUserRequest request) {
    UserEntity user = getById(id);
    String email = normalizeEmail(request.getEmail());
    String mobile = trimToNull(request.getMobileNumber());

    if (user.getRole() != UserRole.ADMIN && mobile == null) {
      throw new BadRequestException("Parents and tutors need a mobile number");
    }
    if (user.getRole() == UserRole.ADMIN && email == null) {
      throw new BadRequestException("Admins need an email");
    }
    assertMobileFree(mobile, id);
    assertEmailFree(email, id);

    if (!Objects.equals(mobile, user.getMobileNumber())) {
      user.setMobileVerified(false);
    }
    user.setFullName(request.getFullName().trim());
    user.setMobileNumber(mobile);
    user.setEmail(email);
    user.setGender(userMapper.toEntityGender(request.getGender()));
    user.setPreferredLanguage(userMapper.toEntityLanguage(request.getPreferredLanguage()));
    user.setCity(trimToNull(request.getCity()));
    user.setPincode(trimToNull(request.getPincode()));
    user.setProfilePhotoUrl(trimToNull(request.getProfilePhotoUrl()));
    return userRepository.save(user);
  }

  @Transactional
  public UserEntity updateStatus(Long actingUserId, Long id, UserStatus status, String reason) {
    if (actingUserId.equals(id)) {
      throw new BadRequestException("You cannot change your own status");
    }
    UserEntity user = getById(id);
    user.setStatus(status);
    user.setStatusReason(status == UserStatus.ACTIVE ? null : trimToNull(reason));
    return userRepository.save(user);
  }

  @Transactional
  public void delete(Long actingUserId, Long id) {
    if (actingUserId.equals(id)) {
      throw new BadRequestException("You cannot delete your own account");
    }
    UserEntity user = getById(id);
    user.setStatus(UserStatus.DELETED);
    userRepository.save(user);
  }

  public UserSummary summary() {
    return new UserSummary()
        .totalUsers(userRepository.countByStatusNot(UserStatus.DELETED))
        .parents(userRepository.countByRoleAndStatusNot(UserRole.PARENT, UserStatus.DELETED))
        .tutors(userRepository.countByRoleAndStatusNot(UserRole.TUTOR, UserStatus.DELETED))
        .admins(userRepository.countByRoleAndStatusNot(UserRole.ADMIN, UserStatus.DELETED))
        .blocked(userRepository.countByStatus(UserStatus.BLOCKED))
        .newThisWeek(
            userRepository.countByStatusNotAndCreatedAtAfter(
                UserStatus.DELETED, LocalDateTime.now(clock).minusDays(7)));
  }

  // ---------- helpers ----------

  private void assertMobileFree(String mobile, Long exceptUserId) {
    if (mobile == null) return;
    boolean taken =
        exceptUserId == null
            ? userRepository.existsByMobileNumber(mobile)
            : userRepository.existsByMobileNumberAndIdNot(mobile, exceptUserId);
    if (taken) {
      throw new ConflictException("Mobile number " + mobile + " is already registered");
    }
  }

  private void assertEmailFree(String email, Long exceptUserId) {
    if (email == null) return;
    boolean taken =
        exceptUserId == null
            ? userRepository.existsByEmail(email)
            : userRepository.existsByEmailAndIdNot(email, exceptUserId);
    if (taken) {
      throw new ConflictException("Email " + email + " is already registered");
    }
  }

  static String normalizeEmail(String email) {
    String trimmed = trimToNull(email);
    return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
  }

  static String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
