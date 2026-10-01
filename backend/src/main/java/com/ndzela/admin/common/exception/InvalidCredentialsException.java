package com.ndzela.admin.common.exception;

/** Wrong password, OTP or TOTP code (HTTP 401). */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
