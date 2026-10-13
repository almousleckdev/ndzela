package com.ndzela.admin.common.exception;

// The file storage could not be reached or refused the request (HTTP 502).
public class StorageUnavailableException extends RuntimeException {
    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
