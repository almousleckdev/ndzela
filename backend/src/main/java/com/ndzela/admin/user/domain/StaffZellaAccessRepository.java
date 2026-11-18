package com.ndzela.admin.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StaffZellaAccessRepository extends JpaRepository<StaffZellaAccess, UUID> {
}
