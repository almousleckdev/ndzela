package com.ndzela.admin.driverapp.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, UUID> {
    long countByDriverIdAndCreatedAtAfter(UUID driverId, OffsetDateTime since);
}
