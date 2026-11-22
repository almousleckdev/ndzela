package com.ndzela.admin.common.exception;

// Login challenge, refresh token or reset token is unknown, expired or already used (HTTP 401).
// The code is SESSION_REPLACED when a driver's session was ended by a newer sign-in, so the app can say so.
public class SessionExpiredException extends RuntimeException {
    public static final String EXPIRED = "SESSION_EXPIRED";
    public static final String REPLACED = "SESSION_REPLACED";

    private final String code;

    public SessionExpiredException(String message) {
        this(message, EXPIRED);
    }

    public SessionExpiredException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
