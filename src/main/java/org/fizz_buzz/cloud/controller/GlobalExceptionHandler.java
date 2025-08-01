package org.fizz_buzz.cloud.controller;

import org.fizz_buzz.cloud.dto.MessageDTO;
import org.fizz_buzz.cloud.exception.EmptyPathException;
import org.fizz_buzz.cloud.exception.ForbiddenSymbolException;
import org.fizz_buzz.cloud.exception.ResourceAlreadyExistsException;
import org.fizz_buzz.cloud.exception.ResourceNotFound;
import org.fizz_buzz.cloud.exception.UserAlreadyExists;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageDTO validationExceptionHandling(MethodArgumentNotValidException e) {

        BindingResult bindingResult = e.getBindingResult();
        StringBuilder errorMessageBuilder = new StringBuilder();

        bindingResult
                .getFieldErrors()
                .forEach(error -> errorMessageBuilder.append("Field: %s Error: %s \n"
                        .formatted(error.getField(), error.getDefaultMessage())));

        return new MessageDTO(errorMessageBuilder.toString());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public MessageDTO handleAccessDeniedException(AccessDeniedException e) {

        return new MessageDTO(e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public MessageDTO handleAuthenticationException(AuthenticationException e) {

        return new MessageDTO(e.getMessage());
    }

    @ExceptionHandler({EmptyPathException.class, ForbiddenSymbolException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageDTO handlePathValidationException(RuntimeException e) {

        return new MessageDTO(e.getMessage());
    }

    @ExceptionHandler(ResourceNotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public MessageDTO handleResourceNotFoundException(ResourceNotFound e) {

        return new MessageDTO(e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageDTO handleMissingField(MissingServletRequestParameterException e) {

        return new MessageDTO("Missing parameter \"%s\"".formatted(e.getParameterName()));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public MessageDTO handleResourceAlreadyExistsException(ResourceAlreadyExistsException e) {

        return new MessageDTO(e.getMessage());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageDTO handleHandlerMethodValidationException(HandlerMethodValidationException e) {


        StringBuilder message = new StringBuilder();

        e.getParameterValidationResults()
                .forEach(parameterValidationResult -> parameterValidationResult
                        .getResolvableErrors()
                        .forEach(messageSourceResolvable ->
                                message.append(messageSourceResolvable.getDefaultMessage())
                                        .append("\n")));

        return new MessageDTO(message.toString());
    }
}
