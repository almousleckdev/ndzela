package com.ndzela.admin.driver.domain;

import java.util.UUID;

// Published once a driver's email or phone changed; other features react after the commit.
public record DriverContactChangedEvent(UUID driverId, UUID countryId) {}
