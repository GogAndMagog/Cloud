package org.fizz_buzz.cloud.exception;

public class ForbiddenSymbolException extends RuntimeException{

    private static final String message =
            "File path must not contains this symbols in directory names: /,\\,?,*,:,<,>,\",|";

    public ForbiddenSymbolException() {
        super(message);
    }
}
