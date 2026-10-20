package com.ndzela.admin.auth.application;

import com.ndzela.admin.auth.api.dto.LoginStep;
import com.ndzela.admin.auth.api.dto.LoginStepResponse;
import com.ndzela.admin.auth.application.model.AuthTokens;
import com.ndzela.admin.auth.application.model.LoginChallenge;
import com.ndzela.admin.auth.application.model.LoginStage;
import com.ndzela.admin.common.exception.InvalidCredentialsException;
import com.ndzela.admin.common.exception.SessionExpiredException;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Signing in: password, one-time code and the authenticator step
class AuthLoginTest extends AbstractAuthServiceTest {
    @Test
    void startLoginWithValidPasswordSendsOtpAndOpensChallenge() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(otpService.generateAndStore(EMAIL)).thenReturn("123456");
        when(challenges.start(EMAIL)).thenReturn("challenge-1");

        LoginStepResponse response = authService.startLogin("  Test@Ndzela.com ", "secret");

        assertEquals("challenge-1", response.challengeToken());
        assertEquals(LoginStep.EMAIL_OTP, response.nextStep());
        verify(emailProvider).sendHtmlEmail(eq(EMAIL), anyString(), eq("otp-email"), anyMap());
    }

    @Test
    void startLoginWithWrongPasswordCountsFailureAndSendsNoOtp() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.startLogin(EMAIL, "bad"));

        verify(loginAttempts).loginFailed(EMAIL);
        verifyNoInteractions(otpService, emailProvider);
    }

    @Test
    void startLoginForUnknownEmailStillHashesAndFailsGenerically() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.startLogin(EMAIL, "whatever"));

        verify(passwordEncoder).matches("whatever", "decoy-hash");
        verifyNoInteractions(otpService, emailProvider);
    }

    @Test
    void verifyOtpAdvancesToMfaVerifyForEnrolledUser() {
        LoginChallenge challenge = new LoginChallenge("c", EMAIL, LoginStage.PASSWORD_VERIFIED);
        when(challenges.require("c", LoginStage.PASSWORD_VERIFIED)).thenReturn(challenge);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(otpService.verify(EMAIL, "123456")).thenReturn(true);

        LoginStepResponse response = authService.verifyOtp("c", "123456");

        assertEquals(LoginStep.MFA_VERIFY, response.nextStep());
        verify(challenges).advance(challenge, LoginStage.OTP_VERIFIED);
    }

    @Test
    void verifyOtpRoutesUnenrolledUserToMfaSetup() {
        user.setMfaEnabled(false);
        LoginChallenge challenge = new LoginChallenge("c", EMAIL, LoginStage.PASSWORD_VERIFIED);
        when(challenges.require("c", LoginStage.PASSWORD_VERIFIED)).thenReturn(challenge);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(otpService.verify(EMAIL, "123456")).thenReturn(true);

        assertEquals(LoginStep.MFA_SETUP, authService.verifyOtp("c", "123456").nextStep());
    }

    @Test
    void wrongOtpCountsFailureAndDoesNotAdvance() {
        LoginChallenge challenge = new LoginChallenge("c", EMAIL, LoginStage.PASSWORD_VERIFIED);
        when(challenges.require("c", LoginStage.PASSWORD_VERIFIED)).thenReturn(challenge);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(otpService.verify(EMAIL, "000000")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.verifyOtp("c", "000000"));

        verify(loginAttempts).loginFailed(EMAIL);
        verify(challenges, never()).advance(any(), any());
    }

    @Test
    void totpStepRequiresCompletedOtpStage() {
        when(challenges.require("c", LoginStage.OTP_VERIFIED)).thenThrow(new SessionExpiredException("expired"));

        assertThrows(SessionExpiredException.class, () -> authService.completeLogin("c", "123456"));

        verifyNoInteractions(mfaService, jwtService, refreshTokens);
    }

    @Test
    void validTotpIssuesTokensAndClosesChallenge() {
        LoginChallenge challenge = new LoginChallenge("c", EMAIL, LoginStage.OTP_VERIFIED);
        when(challenges.require("c", LoginStage.OTP_VERIFIED)).thenReturn(challenge);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(mfaService.verifyLoginCode(user, "123456")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokens.issue(eq(EMAIL), any())).thenReturn("refresh");

        AuthTokens tokens = authService.completeLogin("c", "123456");

        assertEquals("access", tokens.accessToken());
        assertEquals("refresh", tokens.refreshToken());
        verify(challenges).finish("c");
        verify(loginAttempts).loginSucceeded(EMAIL);
        verify(audit).recordSignIn(EMAIL, true, null, null);
    }

    @Test
    void validTotpStampsAFreshSessionAndRevokesOldRefreshTokensBeforeIssuingANewOne() {
        LoginChallenge challenge = new LoginChallenge("c", EMAIL, LoginStage.OTP_VERIFIED);
        when(challenges.require("c", LoginStage.OTP_VERIFIED)).thenReturn(challenge);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(mfaService.verifyLoginCode(user, "123456")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokens.issue(eq(EMAIL), any())).thenReturn("refresh");

        authService.completeLogin("c", "123456");

        assertNotNull(user.getSessionId());
        var inOrder = inOrder(refreshTokens);
        inOrder.verify(refreshTokens).revokeAll(EMAIL);
        inOrder.verify(refreshTokens).issue(eq(EMAIL), any());
        verify(jwtService).generateAccessToken(any(), eq(user.getSessionId()));
    }

    @Test
    void invalidTotpCountsFailureAndIssuesNothing() {
        LoginChallenge challenge = new LoginChallenge("c", EMAIL, LoginStage.OTP_VERIFIED);
        when(challenges.require("c", LoginStage.OTP_VERIFIED)).thenReturn(challenge);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(mfaService.verifyLoginCode(user, "111111")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.completeLogin("c", "111111"));

        verify(loginAttempts).loginFailed(EMAIL);
        verify(challenges, never()).finish(anyString());
        verifyNoInteractions(jwtService, refreshTokens);
    }
}
