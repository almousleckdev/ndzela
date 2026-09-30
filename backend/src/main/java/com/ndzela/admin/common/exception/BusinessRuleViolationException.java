package com.ndzela.admin.common.exception;

import lombok.Getter;

/** A request that is well-formed but violates a domain rule (HTTP 422). */
@Getter
public class BusinessRuleViolationException extends RuntimeException {

    public static final String DEFAULT_CODE = "BUSINESS_RULE_VIOLATION";

    private final String code;

    public BusinessRuleViolationException(String message) {
        this(DEFAULT_CODE, message);
    }

    public BusinessRuleViolationException(String code, String message) {
        super(message);
        this.code = code;
    }
}
