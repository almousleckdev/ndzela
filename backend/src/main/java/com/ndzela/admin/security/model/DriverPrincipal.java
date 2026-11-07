package com.ndzela.admin.security.model;

import com.ndzela.admin.driver.domain.DriverStatus;

import java.security.Principal;
import java.util.UUID;

// The authenticated driver of a driver-app request; what they may do comes from their status. Its name is what the audit trail shows as the actor, never the record's internals.
public record DriverPrincipal(UUID driverId, UUID countryId, DriverStatus status, boolean mustChangePassword, String fullName) implements Principal {
    public static final String ACTOR_PREFIX = "Chauffeur · ";

    @Override
    public String getName() {
        return ACTOR_PREFIX + fullName;
    }

    @Override
    public String toString() {
        return getName();
    }
}
