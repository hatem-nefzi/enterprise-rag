package com.rag.backend.dto.document;

import com.rag.backend.model.enums.DocumentStatus;

import lombok.Data;
@Data
public
class DocumentUploadResponse {
    private Long documentId;
    private String filename;
    private DocumentStatus status;
    private String message;
    private Long uploadedAt;
  

    public static DocumentUploadResponse success(Long documentId, String filename){
        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setDocumentId(documentId);
        response.setFilename(filename);
        response.setStatus(DocumentStatus.PROCESSING);
        response.setMessage("Document uploaded successfully and queued for processing.");
        response.setUploadedAt(System.currentTimeMillis());
        return response;
    }

    public static DocumentUploadResponse error(String filename, String message) {
        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setFilename(filename);
        response.setStatus(DocumentStatus.FAILED);
        response.setMessage(message);
        response.setUploadedAt(System.currentTimeMillis());
        return response;
    }
}



