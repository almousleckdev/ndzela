package com.ndzela.admin.infrastructure.zella;

// The Zella app could not be reached or answered something we cannot use
public class ZellaException extends RuntimeException {
    public ZellaException(String message, Throwable cause) {
        super(message, cause);
    }
}
