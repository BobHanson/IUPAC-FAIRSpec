package com.junrar.exception;

public class RarException extends Exception {
    public RarException(Throwable cause) {
        super(cause);
    }

    public RarException() {
    }

    public RarException(String message) {
        super(message);
    }
}
