package org.fizz_buzz.cloud.exception;

public class EmptyPathException extends RuntimeException{

    private static final String message = "Path must not be empty or contains empty directories";

    public EmptyPathException() {

        super(message);
    }
}
