package com.ndzela.admin.security.jwt;

import java.util.UUID;

public record DriverClaims(UUID driverId, UUID countryId, String sessionId) {}
