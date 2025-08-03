package org.fizz_buzz.cloud.exception;

public class NotDirectoryException extends RuntimeException{

    private static final String MESSAGE = "\"%s\" not a directory";

    public NotDirectoryException(String path) {

        super(MESSAGE.formatted(path));
    }
}
