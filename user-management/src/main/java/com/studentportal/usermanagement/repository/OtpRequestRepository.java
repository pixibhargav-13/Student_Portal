package com.studentportal.usermanagement.repository;

import com.studentportal.usermanagement.model.entity.OtpRequestEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRequestRepository extends JpaRepository<OtpRequestEntity, Long> {

  Optional<OtpRequestEntity> findTopByMobileNumberOrderByCreatedAtDesc(String mobileNumber);

  Optional<OtpRequestEntity> findTopByMobileNumberAndConsumedFalseOrderByCreatedAtDesc(
      String mobileNumber);

  long countByMobileNumberAndCreatedAtAfter(String mobileNumber, LocalDateTime after);

  @Modifying
  @Query(
      "update OtpRequestEntity o set o.consumed = true"
          + " where o.mobileNumber = :mobileNumber and o.consumed = false")
  int consumeAllActive(String mobileNumber);

  @Modifying
  @Query("delete from OtpRequestEntity o where o.createdAt < :before")
  int deleteOlderThan(LocalDateTime before);
}
