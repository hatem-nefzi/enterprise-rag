package com.rag.backend.dto.document;
import com.rag.backend.model.enums.DocumentStatus;

import lombok.Data;

@Data
public class DocumentStatusResponse{
    private Long documentId;
    private String filename;
    private DocumentStatus status;
    private String errorMessage;
    private Integer chunksProcessed;
    private long uploadedAt;
    private long processedAt;
}


