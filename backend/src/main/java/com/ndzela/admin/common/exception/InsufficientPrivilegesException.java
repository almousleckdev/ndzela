package com.ndzela.admin.common.exception;

/** The caller is allowed to use the endpoint but not to do this particular thing (HTTP 403). */
public class InsufficientPrivilegesException extends RuntimeException {
    public InsufficientPrivilegesException(String message) {
        super(message);
    }
}
