package com.ndzela.admin.security.model;

import com.ndzela.admin.driver.domain.DriverStatus;

import java.util.UUID;

// The authenticated driver of a driver-app request; what they may do comes from their status
public record DriverPrincipal(UUID driverId, UUID countryId, DriverStatus status, boolean mustChangePassword) {}
