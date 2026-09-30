package com.ndzela.admin.common.exception;

/** The caller's own country is inactive (HTTP 403). */
public class CountryInactiveException extends RuntimeException {
    public CountryInactiveException() {
        super("Your country is not active.");
    }
}
