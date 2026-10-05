package com.studentportal.usermanagement.sms;

/**
 * Sends SMS. Swap {@link LoggingSmsSender} for a real provider (MSG91 / Twilio / Fast2SMS) by
 * adding another implementation and marking it {@code @Primary} or profile-specific.
 */
public interface SmsSender {
  void send(String mobileNumber, String message);
}
