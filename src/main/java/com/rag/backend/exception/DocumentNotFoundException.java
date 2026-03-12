package com.rag.backend.exception;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(Long DocumentId) {
        super("Document not found : " + DocumentId);
    }
    
}
