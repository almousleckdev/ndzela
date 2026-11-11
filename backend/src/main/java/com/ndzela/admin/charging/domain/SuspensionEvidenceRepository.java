package com.ndzela.admin.charging.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SuspensionEvidenceRepository extends JpaRepository<SuspensionEvidence, UUID> {
    boolean existsByEventId(String eventId);

    Page<SuspensionEvidence> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
