package org.fizz_buzz.cloud.exception;

public class ResourceNotFound extends RuntimeException{

    private static final String MESSAGE = "Resource: \"%s\" not found";

    public ResourceNotFound(String resourcePath) {

        super(MESSAGE.formatted(resourcePath));
    }
}
