package com.ndzela.admin.common.exception;

// Account is temporarily locked or disabled (HTTP 423).
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
