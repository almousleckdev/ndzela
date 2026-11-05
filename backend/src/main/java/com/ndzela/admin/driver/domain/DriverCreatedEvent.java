package com.ndzela.admin.driver.domain;

import java.util.UUID;

// Published once a driver is saved; other features react after the commit.
public record DriverCreatedEvent(UUID driverId, UUID countryId) {}
