package com.rag.backend.model;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "chunks")
@Data
public class Chunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "document_id")
    private Document document;
    
    @Column(name = "chunk_index")
    private Integer chunkIndex;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    // NOTE: pgvector handled separately via custom repository
    // Don't map embedding as JPA field (use native queries)
    
    @Column(name = "token_count")
    private Integer tokenCount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}