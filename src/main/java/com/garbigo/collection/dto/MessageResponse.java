package com.garbigo.collection.dto;

/**
 * Plain {"message": "..."} error/response body, used by
 * {@link com.garbigo.collection.exception.GlobalExceptionHandler}.
 *
 * <p>TODO: inferred from how auth-service's GlobalExceptionHandler
 * constructs and reads it ({@code new MessageResponse(ex.getMessage())}) -
 * auth-service's actual {@code com.garbigo.auth.dto.MessageResponse} wasn't
 * one of the shared files. Replace this with that one if its shape differs
 * (e.g. if it also carries a status code or timestamp field), so error
 * responses are byte-for-byte consistent across both services.
 */
public class MessageResponse {

    private String message;

    public MessageResponse() {
    }

    public MessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}