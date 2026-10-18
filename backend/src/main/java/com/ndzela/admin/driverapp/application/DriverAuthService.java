package com.ndzela.admin.driverapp.application;

import com.ndzela.admin.auth.application.RefreshTokenService;
import com.ndzela.admin.auth.application.model.TokenRotation;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.InvalidCredentialsException;
import com.ndzela.admin.common.exception.SessionExpiredException;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.driver.domain.LoginCandidate;
import com.ndzela.admin.driverapp.api.dto.DriverTokens;
import com.ndzela.admin.security.jwt.JwtService;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// Sign-in, session renewal and password change for drivers
@Service
public class DriverAuthService {
    private static final String INVALID_LOGIN = "Invalid phone, email or password.";
    private static final String SUBJECT_PREFIX = "driver:";
    private static final int MIN_LOCAL_DIGITS = 8;

    private final DriverRepository drivers;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final DriverLoginAttempts attempts;
    private final String unknownDriverHash;

    public DriverAuthService(DriverRepository drivers, PasswordEncoder encoder, JwtService jwt, RefreshTokenService refreshTokens, DriverLoginAttempts attempts) {
        this.drivers = drivers;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.attempts = attempts;
        this.unknownDriverHash = encoder.encode("no-such-driver");
    }

    // Deliberately not transactional: it runs in the driver's country, which is only known after the lookup.
    public DriverTokens login(String identifier, String password) {
        LoginCandidate candidate = findCandidate(identifier.trim());
        if (candidate == null) {
            encoder.matches(password, unknownDriverHash);
            throw new InvalidCredentialsException(INVALID_LOGIN);
        }
        return TenantHolder.callAs(TenantContext.country(candidate.getCountryId()), () -> authenticate(candidate.getId(), password));
    }

    public DriverTokens refresh(String refreshToken) {
        TokenRotation rotation = refreshTokens.rotate(refreshToken);
        String[] subject = rotation.email().split(":");
        if (subject.length != 3 || !SUBJECT_PREFIX.equals(subject[0] + ":")) {
            refreshTokens.revoke(rotation.newToken());
            throw new SessionExpiredException("Invalid or expired session.");
        }
        UUID countryId = UUID.fromString(subject[1]);
        UUID driverId = UUID.fromString(subject[2]);
        Driver driver = TenantHolder.callAs(TenantContext.country(countryId), () -> drivers.findById(driverId).orElse(null));
        if (driver == null || !canUseTheApp(driver.getStatus())) {
            refreshTokens.revoke(rotation.newToken());
            throw new SessionExpiredException("Invalid or expired session.");
        }
        return new DriverTokens(jwt.generateDriverAccessToken(driverId, countryId, driver.getSessionId()), rotation.newToken(), jwt.accessTokenTtlSeconds(), driver.isMustChangePassword());
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokens.revoke(refreshToken);
        }
    }

    @Transactional
    public DriverTokens changePassword(UUID driverId, String currentPassword, String newPassword) {
        // Read without the row lock: the failure is counted in its own transaction, which locks the same row.
        Driver driver = drivers.findById(driverId).orElseThrow();
        attempts.assertNotLocked(driver);
        if (!encoder.matches(currentPassword, driver.getPasswordHash())) {
            attempts.failed(driverId);
            throw new BusinessRuleViolationException("PASSWORD_INCORRECT", "The current password is wrong.");
        }
        driver = drivers.findForUpdate(driverId).orElseThrow();
        if (encoder.matches(newPassword, driver.getPasswordHash())) {
            throw new BusinessRuleViolationException("PASSWORD_UNCHANGED", "The new password must differ from the current one.");
        }
        driver.setPasswordHash(encoder.encode(newPassword));
        driver.setMustChangePassword(false);
        refreshTokens.revokeAll(subjectOf(driver));
        return tokensFor(driver);
    }

    // Sets a new password after the identity was proven by e-mail code
    @Transactional
    public void setPassword(UUID driverId, String newPassword) {
        Driver driver = drivers.findForUpdate(driverId).orElseThrow();
        driver.setPasswordHash(encoder.encode(newPassword));
        driver.setMustChangePassword(false);
        driver.setFailedLoginAttempts(0);
        driver.setLoginLockedUntil(null);
        refreshTokens.revokeAll(subjectOf(driver));
    }

    private DriverTokens authenticate(UUID driverId, String password) {
        Driver driver = drivers.findById(driverId).orElseThrow();
        attempts.assertNotLocked(driver);
        if (!encoder.matches(password, driver.getPasswordHash())) {
            attempts.failed(driverId);
            throw new InvalidCredentialsException(INVALID_LOGIN);
        }
        if (driver.getStatus() == DriverStatus.BLOCKED) {
            throw new BusinessRuleViolationException("DRIVER_BLOCKED", "This driver account is blocked.");
        }
        if (!canUseTheApp(driver.getStatus()) || !driver.getCountry().isActive()) {
            throw new InvalidCredentialsException(INVALID_LOGIN);
        }
        attempts.succeeded(driverId);
        return tokensFor(driver);
    }

    private DriverTokens tokensFor(Driver driver) {
        return new DriverTokens(jwt.generateDriverAccessToken(driver.getId(), driver.getCountryId(), driver.getSessionId()), refreshTokens.issue(subjectOf(driver)),
                jwt.accessTokenTtlSeconds(), driver.isMustChangePassword());
    }

    // A phone is matched on its end, so "074 11 11 11" finds "+24174111111"; an ambiguous number matches nobody.
    LoginCandidate findCandidate(String identifier) {
        List<LoginCandidate> found;
        if (identifier.contains("@")) {
            found = drivers.findLoginCandidatesByEmail(identifier);
        } else {
            String digits = identifier.replaceAll("\\D", "");
            String local = digits.replaceFirst("^0+", "");
            if (local.length() < MIN_LOCAL_DIGITS) {
                return null;
            }
            found = drivers.findLoginCandidatesByPhone("+" + digits, local);
        }
        return found.size() == 1 ? found.get(0) : null;
    }

    private static String subjectOf(Driver driver) {
        return SUBJECT_PREFIX + driver.getCountryId() + ":" + driver.getId();
    }

    private static boolean canUseTheApp(DriverStatus status) {
        return status == DriverStatus.ACTIVE || status == DriverStatus.SUSPENDED;
    }
}
