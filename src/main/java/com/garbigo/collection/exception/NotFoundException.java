package com.garbigo.collection.exception;

/**
 * Maps to 404 in GlobalExceptionHandler. Extends CustomException so
 * anything already catching that still works, but is handled by its own,
 * more specific handler first - a missing resource shouldn't come back as
 * the 400 every other CustomException gets.
 */
@SuppressWarnings("serial")
public class NotFoundException extends CustomException {
    public NotFoundException(String message) {
        super(message);
    }
}