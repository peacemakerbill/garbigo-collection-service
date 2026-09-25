package com.garbigo.collection.dto;

/** Matches auth-service's error response shape - {"message": "..."}. */
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