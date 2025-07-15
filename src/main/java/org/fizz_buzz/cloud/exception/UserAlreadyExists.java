package org.fizz_buzz.cloud.exception;

public class UserAlreadyExists extends RuntimeException{

    public UserAlreadyExists(final String msg) {
        super(msg);
    }
}
