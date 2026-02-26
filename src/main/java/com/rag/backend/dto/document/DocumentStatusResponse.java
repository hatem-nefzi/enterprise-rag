package com.rag.backend.dto.document;
import lombok.Data;

@Data
public class DocumentStatusResponse{
    private Long documentId;
    private String filename;
    private String status;
    private String errorMessage;
    private Integer chunksProcessed;
    private long uploadedAt;
    private long processedAt;
}


