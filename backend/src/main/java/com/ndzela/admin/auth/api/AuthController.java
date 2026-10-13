package com.ndzela.admin.auth.api;

import com.ndzela.admin.auth.application.model.AuthTokens;
import com.ndzela.admin.auth.api.dto.AuthResponse;
import com.ndzela.admin.auth.api.dto.ForgotPasswordRequest;
import com.ndzela.admin.auth.api.dto.LoginRequest;
import com.ndzela.admin.auth.api.dto.LoginStepResponse;
import com.ndzela.admin.auth.api.dto.MfaSetupRequest;
import com.ndzela.admin.auth.api.dto.MfaSetupResponse;
import com.ndzela.admin.auth.api.dto.OtpVerifyRequest;
import com.ndzela.admin.auth.api.dto.ResetPasswordRequest;
import com.ndzela.admin.auth.api.dto.TotpVerifyRequest;
import com.ndzela.admin.auth.application.AuthService;
import com.ndzela.admin.auth.application.PasswordResetService;
import com.ndzela.admin.common.exception.SessionExpiredException;
import com.ndzela.admin.common.web.dto.MessageResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenCookie refreshCookie;

    @PostMapping("/login")
    public LoginStepResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.startLogin(request.email(), request.password());
    }

    @PostMapping("/verify-otp")
    public LoginStepResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return authService.verifyOtp(request.challengeToken(), request.otp());
    }

    @PostMapping("/resend-otp")
    public LoginStepResponse resendOtp(@Valid @RequestBody MfaSetupRequest request) {
        return authService.resendOtp(request.challengeToken());
    }

    @PostMapping("/mfa/setup")
    public MfaSetupResponse setupMfa(@Valid @RequestBody MfaSetupRequest request) {
        return authService.beginMfaSetup(request.challengeToken());
    }

    @PostMapping("/verify-totp")
    public AuthResponse verifyTotp(@Valid @RequestBody TotpVerifyRequest request, HttpServletResponse response) {
        return respondWithSession(authService.completeLogin(request.challengeToken(), request.code()), response);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = refreshCookie.read(request)
                .orElseThrow(() -> new SessionExpiredException("No active session."));
        try {
            return respondWithSession(authService.refresh(token), response);
        } catch (SessionExpiredException e) {
            refreshCookie.clear(response);
            throw e;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        refreshCookie.read(request).ifPresent(authService::logout);
        refreshCookie.clear(response);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        passwordResetService.requestReset(request.email(), http.getRemoteAddr());
        return new MessageResponse("If the email exists, a reset link was sent.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.newPassword());
        return new MessageResponse("Password reset successfully.");
    }

    private AuthResponse respondWithSession(AuthTokens tokens, HttpServletResponse response) {
        refreshCookie.write(response, tokens.refreshToken());
        return AuthResponse.bearer(tokens.accessToken(), tokens.expiresInSeconds());
    }
}
