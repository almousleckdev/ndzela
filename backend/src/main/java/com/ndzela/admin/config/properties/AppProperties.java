package com.ndzela.admin.config.properties;

import com.ndzela.admin.config.properties.AuthProperties;
import com.ndzela.admin.config.properties.BootstrapProperties;
import com.ndzela.admin.config.properties.BruteForceProperties;
import com.ndzela.admin.config.properties.JwtProperties;
import com.ndzela.admin.config.properties.MfaProperties;
import com.ndzela.admin.config.properties.OtpProperties;
import com.ndzela.admin.config.properties.StorageProperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        List<String> corsAllowedOrigins,
        JwtProperties jwt,
        AuthProperties auth,
        OtpProperties otp,
        BruteForceProperties bruteForce,
        MfaProperties mfa,
        BootstrapProperties bootstrap,
        StorageProperties storage
) {
}
