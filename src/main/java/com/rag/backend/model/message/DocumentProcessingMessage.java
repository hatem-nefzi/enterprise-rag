package com.rag.backend.model.message;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessingMessage {
    // unique ID for tracking this specific message through the pipeline
    private String messageId;
    // the document's ID in your database
    private Long documentId;
    //sanitized filename
    private String filename;
    // where the file lives on disk (or S3 key later)
    private String storagePath;
    // MIME type detected by Tika — not from browser, from your validator
    private String mimeType;

    private long fileSizeBytes;

    private Long uploadedBy;
    private LocalDateTime uploadedAt;
    // tracks how many times processing was attempted
    // useful when inspecting DLQ messages to know how many times it failed
    private int retryCount; 
    
}
