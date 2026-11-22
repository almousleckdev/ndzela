package com.ndzela.admin.freecharging.application;

import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

// Where the free Sunday sessions are read from; the charging module supplies them.
public interface FreeChargingUsageSource {
    Page<FreeChargingUse> page(int page, int size);

    Map<UUID, Instant> lastUseOf(Collection<UUID> driverIds);
}
