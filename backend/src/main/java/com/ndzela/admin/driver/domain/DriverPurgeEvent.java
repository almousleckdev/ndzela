package com.ndzela.admin.driver.domain;

import java.util.Map;
import java.util.UUID;

// Published inside the transaction that erases a driver with their money; each feature removes its own rows and reports what it erased.
public record DriverPurgeEvent(UUID driverId, Map<String, Long> erased) {}
