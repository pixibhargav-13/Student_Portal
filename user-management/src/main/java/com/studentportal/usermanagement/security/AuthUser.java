package com.studentportal.usermanagement.security;

import com.studentportal.usermanagement.model.entity.UserEntity;
import com.studentportal.usermanagement.model.entity.UserRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The logged-in user as seen by Spring Security. Inject it in controllers with
 * {@code @AuthenticationPrincipal AuthUser user} or read it via {@link CurrentUser}.
 */
public record AuthUser(Long id, UserRole role, boolean active) implements UserDetails {

  public static AuthUser from(UserEntity user) {
    return new AuthUser(user.getId(), user.getRole(), user.isActive());
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() {
    return null;
  }

  /** The JWT subject is the user id, so mobile/email can change without invalidating tokens. */
  @Override
  public String getUsername() {
    return String.valueOf(id);
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return active;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
