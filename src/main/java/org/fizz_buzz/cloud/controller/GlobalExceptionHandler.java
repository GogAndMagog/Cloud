package org.fizz_buzz.cloud.controller;

import lombok.extern.slf4j.Slf4j;
import org.fizz_buzz.cloud.dto.response.ErrorMessageResponseDto;
import org.fizz_buzz.cloud.exception.EmptyPathException;
import org.fizz_buzz.cloud.exception.ForbiddenSymbolException;
import org.fizz_buzz.cloud.exception.NotDirectoryException;
import org.fizz_buzz.cloud.exception.ResourceAlreadyExistsException;
import org.fizz_buzz.cloud.exception.ResourceNotFoundException;
import org.fizz_buzz.cloud.exception.UserAlreadyExists;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorMessageResponseDto exceptionHandling(Exception e) {
        log.debug("Unhandled exception was thrown, message: {}", e.getMessage());

        return new ErrorMessageResponseDto("Internal server error");
    }

    @ExceptionHandler(UserAlreadyExists.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorMessageResponseDto eserAlreadyExistsHandling(Exception e) {
        return new ErrorMessageResponseDto(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageResponseDto validationExceptionHandling(MethodArgumentNotValidException e) {
        String errorsMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> "Field: %s Error: %s".formatted(fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.joining("\n"));

        return new ErrorMessageResponseDto(errorsMessage);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageResponseDto handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        String errorsMessage = e.getParameterValidationResults().stream()
                .map(ParameterValidationResult::getResolvableErrors)
                .flatMap(List::stream)
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("\n"));

        return new ErrorMessageResponseDto(errorsMessage);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorMessageResponseDto handleAccessDeniedException(AccessDeniedException e) {
        return new ErrorMessageResponseDto(e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorMessageResponseDto handleAuthenticationException(AuthenticationException e) {
        return new ErrorMessageResponseDto(e.getMessage());
    }

    @ExceptionHandler({EmptyPathException.class, ForbiddenSymbolException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageResponseDto handlePathValidationException(RuntimeException e) {
        return new ErrorMessageResponseDto(e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorMessageResponseDto handleResourceNotFoundException(ResourceNotFoundException e) {
        return new ErrorMessageResponseDto(e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageResponseDto handleMissingField(MissingServletRequestParameterException e) {
        return new ErrorMessageResponseDto("Missing parameter \"%s\"".formatted(e.getParameterName()));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorMessageResponseDto handleResourceAlreadyExistsException(ResourceAlreadyExistsException e) {
        return new ErrorMessageResponseDto(e.getMessage());
    }

    @ExceptionHandler(NotDirectoryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageResponseDto handleNotDirectoryException(NotDirectoryException e) {
        return new ErrorMessageResponseDto(e.getMessage());
    }
}
