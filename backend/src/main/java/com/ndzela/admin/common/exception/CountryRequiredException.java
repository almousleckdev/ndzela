package com.ndzela.admin.common.exception;

/** The operation needs exactly one country but none is selected (HTTP 422, code COUNTRY_REQUIRED). */
public class CountryRequiredException extends BusinessRuleViolationException {
    public static final String CODE = "COUNTRY_REQUIRED";

    public CountryRequiredException() {
        this("Select a country to do this.");
    }

    public CountryRequiredException(String message) {
        super(CODE, message);
    }
}
