package com.rag.backend.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.rag.backend.model.enums.DocumentStatus;


@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    
    private String filename;


    @Column(name = "storage_path")
    private String storagePath; 
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "mime_type")
    private String mimeType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DocumentStatus status ;
    
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt ;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "error_message")
    private String errorMessage;
}