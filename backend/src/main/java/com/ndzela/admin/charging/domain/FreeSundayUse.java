package com.ndzela.admin.charging.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FreeSundayUse(UUID sessionId, String customerId, OffsetDateTime startedAt, Long energyWh, String stopReason) {}
