package org.fizz_buzz.cloud.exception;

public class DirectoryNotExistException extends RuntimeException{

    private static final String message = "Directory: %s does not exists";

    public DirectoryNotExistException(String directoryName) {

        super(message.formatted(directoryName));
    }
}
