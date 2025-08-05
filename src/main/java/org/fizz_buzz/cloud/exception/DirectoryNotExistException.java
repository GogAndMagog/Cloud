package org.fizz_buzz.cloud.exception;

public class DirectoryNotExistException extends RuntimeException{

    private static final String MESSAGE = "\"%s\" is not a directory";

    public DirectoryNotExistException(String path) {

        super(MESSAGE.formatted(path));
    }
}
