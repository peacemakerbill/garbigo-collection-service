package com.garbigo.collection.exception;

import org.springframework.http.HttpStatus;

/**
 * Mirrors auth-service's CustomException pattern - a runtime exception
 * carrying the HTTP status {@link GlobalExceptionHandler} should respond
 * with.
 *
 * <p>TODO: reconcile with auth-service's actual CustomException once
 * shared - this may need to match its constructor shape/fields exactly if
 * any shared client code expects a specific error response format.
 */
public class CustomException extends RuntimeException {

    private final HttpStatus status;

    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
