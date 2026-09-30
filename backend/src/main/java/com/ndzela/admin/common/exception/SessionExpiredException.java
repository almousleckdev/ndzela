package com.ndzela.admin.common.exception;



/** Login challenge, refresh token or reset token is unknown, expired or already used (HTTP 401). */
public class SessionExpiredException extends RuntimeException {
    public SessionExpiredException(String message) {
        super(message);
    }
}
