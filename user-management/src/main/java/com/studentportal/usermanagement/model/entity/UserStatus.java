package com.studentportal.usermanagement.model.entity;

/** Account state. Only ACTIVE users can log in; DELETED is a soft delete. */
public enum UserStatus {
  ACTIVE,
  BLOCKED,
  DELETED
}
