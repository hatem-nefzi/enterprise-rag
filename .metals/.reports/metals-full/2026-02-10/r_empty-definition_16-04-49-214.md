error id: file://<WORKSPACE>/src/main/java/com/rag/backend/model/Document.java:_empty_/GenerationType#
file://<WORKSPACE>/src/main/java/com/rag/backend/model/Document.java
empty definition using pc, found symbol in pc: _empty_/GenerationType#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 230
uri: file://<WORKSPACE>/src/main/java/com/rag/backend/model/Document.java
text:
```scala
package com.rag.backend.model;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
public class Document {
    @Id
    @GeneratedValue(strategy = Generation@@Type.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private String filename;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "mime_type")
    private String mimeType;
    
    private String status = "PROCESSING";
    
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt = LocalDateTime.now();
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "error_message")
    private String errorMessage;
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/GenerationType#