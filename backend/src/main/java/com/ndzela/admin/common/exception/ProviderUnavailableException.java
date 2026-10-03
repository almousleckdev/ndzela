package com.ndzela.admin.common.exception;

// An outside provider (mobile money) could not be reached or refused the request (HTTP 502).
public class ProviderUnavailableException extends RuntimeException {
    public ProviderUnavailableException(String message) {
        super(message);
    }
}
