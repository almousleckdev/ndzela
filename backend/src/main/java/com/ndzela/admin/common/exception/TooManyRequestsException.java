package com.ndzela.admin.common.exception;

// A rate limit was exceeded (HTTP 429).
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
