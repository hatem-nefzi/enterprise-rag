package com.rag.backend.dto.document;

import lombok.Data;
@Data
class DocumentUploadResponse {
    private Long documentId;
    private String filename;
    private String status;
    private String message;
    private Long uploadedAt;
  

    public static DocumentUploadResponse success(Long documentId, String filename){
        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setDocumentId(documentId);
        response.setFilename(filename);
        response.setStatus("PROCESSING");
        response.setMessage("Document uploaded successfully and queued for processing.");
        response.setUploadedAt(System.currentTimeMillis());
        return response;
    }

    public static DocumentUploadResponse error(String filename, String message) {
        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setFilename(filename);
        response.setStatus("FAILED");
        response.setMessage(message);
        response.setUploadedAt(System.currentTimeMillis());
        return response;
    }
}



