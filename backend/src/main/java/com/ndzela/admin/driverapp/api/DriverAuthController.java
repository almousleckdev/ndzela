package com.ndzela.admin.driverapp.api;

import com.ndzela.admin.driverapp.api.dto.ChangePasswordRequest;
import com.ndzela.admin.driverapp.api.dto.DriverLoginRequest;
import com.ndzela.admin.driverapp.api.dto.ForgotPasswordRequest;
import com.ndzela.admin.driverapp.api.dto.ResetPasswordRequest;
import com.ndzela.admin.driverapp.api.dto.VerifyResetCodeRequest;
import com.ndzela.admin.driverapp.application.DriverPasswordResetService;
import com.ndzela.admin.driverapp.api.dto.DriverLogoutRequest;
import com.ndzela.admin.driverapp.api.dto.DriverRefreshRequest;
import com.ndzela.admin.driverapp.api.dto.DriverTokens;
import com.ndzela.admin.driverapp.application.DriverAuthService;
import com.ndzela.admin.security.model.DriverPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver")
@RequiredArgsConstructor
public class DriverAuthController {
    private final DriverAuthService auth;
    private final DriverPasswordResetService passwordReset;

    @PostMapping("/auth/login")
    public DriverTokens login(@Valid @RequestBody DriverLoginRequest request) {
        return auth.login(request.identifier(), request.password());
    }

    @PostMapping("/auth/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        passwordReset.requestCode(request.identifier(), http.getRemoteAddr());
    }

    @PostMapping("/auth/verify-reset-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        passwordReset.verifyCode(request.identifier(), request.code());
    }

    @PostMapping("/auth/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordReset.reset(request.identifier(), request.code(), request.newPassword());
    }

    @PostMapping("/auth/refresh")
    public DriverTokens refresh(@Valid @RequestBody DriverRefreshRequest request) {
        return auth.refresh(request.refreshToken());
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('driver:session')")
    public void logout(@RequestBody(required = false) DriverLogoutRequest request) {
        auth.logout(request == null ? null : request.refreshToken());
    }

    @PutMapping("/me/password")
    @PreAuthorize("hasAuthority('driver:session')")
    public DriverTokens changePassword(@AuthenticationPrincipal DriverPrincipal driver, @Valid @RequestBody ChangePasswordRequest request) {
        return auth.changePassword(driver.driverId(), request.currentPassword(), request.newPassword());
    }
}
