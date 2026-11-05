package com.ndzela.admin.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndReadFalse(UUID userId);

    @Modifying
    @Query("update Notification n set n.read = true where n.userId = :userId and n.id = :id")
    void markRead(@Param("userId") UUID userId, @Param("id") UUID id);

    @Modifying
    @Query("update Notification n set n.read = true where n.userId = :userId and n.read = false")
    void markAllRead(@Param("userId") UUID userId);
}
