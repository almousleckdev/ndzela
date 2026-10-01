package com.ndzela.admin.config.properties;

import java.time.Duration;

public record JwtProperties(String secret, String issuer, Duration accessTokenTtl) {}
