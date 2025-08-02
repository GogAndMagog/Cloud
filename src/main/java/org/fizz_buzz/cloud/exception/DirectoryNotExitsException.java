package org.fizz_buzz.cloud.exception;

public class DirectoryNotExitsException extends RuntimeException{

    private static final String MESSAGE = "\"%s\" is not a directory";

    public DirectoryNotExitsException(String path) {

        super(MESSAGE.formatted(path));
    }
}
