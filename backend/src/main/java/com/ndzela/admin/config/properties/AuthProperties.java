package com.ndzela.admin.config.properties;

import com.ndzela.admin.config.properties.RefreshCookieProperties;

import java.time.Duration;

public record AuthProperties(Duration refreshTokenTtl, Duration challengeTtl, Duration passwordResetTtl, Duration invitationTtl, RefreshCookieProperties refreshCookie) {}
