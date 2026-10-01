package com.ndzela.admin.auth.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.auth.api.dto.LoginStep;
import com.ndzela.admin.auth.api.dto.LoginStepResponse;
import com.ndzela.admin.auth.api.dto.MfaSetupResponse;
import com.ndzela.admin.auth.application.model.AuthTokens;
import com.ndzela.admin.auth.application.model.LoginChallenge;
import com.ndzela.admin.auth.application.model.LoginStage;
import com.ndzela.admin.auth.application.model.TokenRotation;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.InvalidCredentialsException;
import com.ndzela.admin.common.exception.SessionExpiredException;
import com.ndzela.admin.infrastructure.email.EmailProvider;
import com.ndzela.admin.security.LoginAttemptService;
import com.ndzela.admin.security.SecurityUser;
import com.ndzela.admin.security.jwt.JwtService;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class AuthService {
    private static final String INVALID_LOGIN = "Invalid email or password.";
    private static final String INVALID_OTP = "Invalid verification code.";

    private final UserRepository userRepository;
    private final LoginChallengeService challenges;
    private final OtpService otpService;
    private final MfaService mfaService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final EmailProvider emailProvider;
    private final LoginAttemptService loginAttempts;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final String decoyPasswordHash;

    public AuthService(UserRepository userRepository, LoginChallengeService challenges, OtpService otpService,
                       MfaService mfaService, JwtService jwtService, RefreshTokenService refreshTokens,
                       EmailProvider emailProvider, LoginAttemptService loginAttempts, PasswordEncoder passwordEncoder,
                       AuditService audit) {
        this.userRepository = userRepository;
        this.challenges = challenges;
        this.otpService = otpService;
        this.mfaService = mfaService;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.emailProvider = emailProvider;
        this.loginAttempts = loginAttempts;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.decoyPasswordHash = passwordEncoder.encode("decoy-password-for-timing");
    }

    public LoginStepResponse startLogin(String rawEmail, String password) {
        String email = normalize(rawEmail);
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            passwordEncoder.matches(password, decoyPasswordHash);
            throw new InvalidCredentialsException(INVALID_LOGIN);
        }
        loginAttempts.assertCanAuthenticate(user);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw failed(email, INVALID_LOGIN);
        }

        sendOtp(email);
        return new LoginStepResponse(challenges.start(email), LoginStep.EMAIL_OTP, otpService.resendCooldownSeconds());
    }

    public LoginStepResponse resendOtp(String challengeToken) {
        LoginChallenge challenge = challenges.require(challengeToken, LoginStage.PASSWORD_VERIFIED);
        loginAttempts.assertCanAuthenticate(loadForChallenge(challenge));
        sendOtp(challenge.email());
        return new LoginStepResponse(challengeToken, LoginStep.EMAIL_OTP, otpService.resendCooldownSeconds());
    }

    public LoginStepResponse verifyOtp(String challengeToken, String otp) {
        LoginChallenge challenge = challenges.require(challengeToken, LoginStage.PASSWORD_VERIFIED);
        User user = loadForChallenge(challenge);
        loginAttempts.assertCanAuthenticate(user);

        boolean valid;
        try {
            valid = otpService.verify(challenge.email(), otp);
        } catch (BusinessRuleViolationException e) {
            throw failed(challenge.email(), e.getMessage());
        }
        if (!valid) {
            throw failed(challenge.email(), INVALID_OTP);
        }
        challenges.advance(challenge, LoginStage.OTP_VERIFIED);
        return new LoginStepResponse(challengeToken, user.isMfaEnabled() ? LoginStep.MFA_VERIFY : LoginStep.MFA_SETUP, 0);
    }

    public MfaSetupResponse beginMfaSetup(String challengeToken) {
        LoginChallenge challenge = challenges.require(challengeToken, LoginStage.OTP_VERIFIED);
        return mfaService.beginSetup(loadForChallenge(challenge));
    }

    public AuthTokens completeLogin(String challengeToken, String code) {
        LoginChallenge challenge = challenges.require(challengeToken, LoginStage.OTP_VERIFIED);
        User user = loadForChallenge(challenge);
        loginAttempts.assertCanAuthenticate(user);

        if (!mfaService.verifyLoginCode(user, code)) {
            throw failed(challenge.email(), INVALID_OTP);
        }
        challenges.finish(challengeToken);
        loginAttempts.loginSucceeded(challenge.email());
        log.info("Login completed for {}", challenge.email());
        audit.recordSignIn(user.getEmail(), true, user.countryIdOrNull(), null);
        return issueTokens(user, refreshTokens.issue(user.getEmail()));
    }

    public AuthTokens refresh(String refreshToken) {
        TokenRotation rotation = refreshTokens.rotate(refreshToken);
        User user = userRepository.findByEmail(rotation.email())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE && u.isCountryActive())
                .orElse(null);
        if (user == null) {
            refreshTokens.revokeAll(rotation.email());
            throw new SessionExpiredException("Invalid or expired session.");
        }
        return issueTokens(user, rotation.newToken());
    }

    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    private void sendOtp(String email) {
        String otp = otpService.generateAndStore(email);
        emailProvider.sendHtmlEmail(email, "NDZela Admin - Login OTP", "otp-email", Map.of("otp", otp));
    }

    private AuthTokens issueTokens(User user, String refreshToken) {
        String accessToken = jwtService.generateAccessToken(new SecurityUser(user));
        return new AuthTokens(accessToken, jwtService.accessTokenTtlSeconds(), refreshToken);
    }

    private User loadForChallenge(LoginChallenge challenge) {
        return userRepository.findByEmail(challenge.email())
                .orElseThrow(() -> new SessionExpiredException("Login session expired. Please sign in again."));
    }

    private InvalidCredentialsException failed(String email, String message) {
        loginAttempts.loginFailed(email);
        audit.recordSignIn(email, false, userRepository.findByEmail(email).map(User::countryIdOrNull).orElse(null), message);
        return new InvalidCredentialsException(message);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
