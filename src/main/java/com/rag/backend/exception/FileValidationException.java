package com.rag.backend.exception;

public class FileValidationException extends RuntimeException {
    public FileValidationException(String message) {
        super(message);
    }
}