package com.ndzela.admin.common.exception;

// A request parameter has an unacceptable value (an unknown sort field...)
public class InvalidParameterException extends RuntimeException {
    public InvalidParameterException(String parameter) {
        super("The parameter '" + parameter + "' has an invalid value.");
    }
}
