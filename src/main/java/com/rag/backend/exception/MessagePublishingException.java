package com.rag.backend.exception;

// exception/MessagePublishingException.java
public class MessagePublishingException extends RuntimeException {
    public MessagePublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}