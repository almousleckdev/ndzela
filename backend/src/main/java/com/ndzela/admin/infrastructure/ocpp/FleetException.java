package com.ndzela.admin.infrastructure.ocpp;

// The Fleet API could not be reached or refused the request
public class FleetException extends RuntimeException {
    public FleetException(String message, Throwable cause) {
        super(message, cause);
    }

    public FleetException(String message) {
        super(message);
    }
}
