package com.studentportal.usermanagement.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Placeholder until an SMS provider is chosen: writes the message to the log instead. */
@Slf4j
@Component
public class LoggingSmsSender implements SmsSender {

  @Override
  public void send(String mobileNumber, String message) {
    log.info("[SMS -> {}] {}", mobileNumber, message);
  }
}
