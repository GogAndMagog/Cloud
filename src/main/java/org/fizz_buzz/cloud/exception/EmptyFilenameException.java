package org.fizz_buzz.cloud.exception;

public class EmptyFilenameException extends RuntimeException {
    public EmptyFilenameException(String message) {
        super(message);
    }
}
