package org.fizz_buzz.cloud.exception;

public class EmptyPathException extends RuntimeException{

    private static final String MESSAGE = "Path must not be empty or contains empty directories";

    public EmptyPathException() {
        super(MESSAGE);
    }
}
