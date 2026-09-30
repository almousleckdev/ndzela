package com.ndzela.admin.common.exception;

/** The country code or id sent by the client is unknown or inactive (HTTP 400). */
public class InvalidCountryException extends RuntimeException {
    public InvalidCountryException() {
        super("Unknown or inactive country.");
    }
}
