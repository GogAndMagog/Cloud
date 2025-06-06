package org.fizz_buzz.cloud.controller;

import org.fizz_buzz.cloud.dto.MessageDTO;
import org.fizz_buzz.cloud.exception.UserAlreadyExists;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public MessageDTO exceptionHandling(Exception e) {

        return new MessageDTO("Internal server error");
    }

    @ExceptionHandler(UserAlreadyExists.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public MessageDTO eserAlreadyExistsHandling(Exception e) {

        return new MessageDTO(e.getMessage());
    }
}
