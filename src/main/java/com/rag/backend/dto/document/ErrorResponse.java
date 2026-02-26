package com.rag.backend.dto.document;
import lombok.Data;

@Data
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private long timestamp;

    public ErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = System.currentTimeMillis();
    }
}