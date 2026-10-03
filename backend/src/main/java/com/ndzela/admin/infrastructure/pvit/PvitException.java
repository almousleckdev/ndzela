package com.ndzela.admin.infrastructure.pvit;

// PVit could not be reached or refused a call.
public class PvitException extends RuntimeException {
    private final boolean notFound;

    public PvitException(String message) {
        super(message);
        this.notFound = false;
    }

    public PvitException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public PvitException(String message, Throwable cause, boolean notFound) {
        super(message, cause);
        this.notFound = notFound;
    }

    // PVit answered that it has no such transaction, as opposed to being unreachable or failing.
    public boolean isNotFound() {
        return notFound;
    }
}
