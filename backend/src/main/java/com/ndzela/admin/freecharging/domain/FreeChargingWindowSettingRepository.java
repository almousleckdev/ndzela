package com.ndzela.admin.freecharging.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FreeChargingWindowSettingRepository extends JpaRepository<FreeChargingWindowSetting, UUID> {
    Optional<FreeChargingWindowSetting> findFirstBy();
}
