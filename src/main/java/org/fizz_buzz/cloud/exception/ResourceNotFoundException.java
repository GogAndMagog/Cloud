package org.fizz_buzz.cloud.exception;

public class ResourceNotFoundException extends RuntimeException{

    private static final String MESSAGE = "Resource: \"%s\" not found";

    public ResourceNotFoundException(String resourcePath) {

        super(MESSAGE.formatted(resourcePath));
    }
}
