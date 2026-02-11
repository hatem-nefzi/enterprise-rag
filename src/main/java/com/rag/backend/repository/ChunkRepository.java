package com.rag.backend.repository;

import com.rag.backend.model.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
interface ChunkRepository extends JpaRepository<Chunk, Long> {
    List<Chunk> findByDocumentId(Long documentId);

    //Vector similarity search (NAtive SQL)
    @Query(value = """
        SELECT c.* FROM chunks c 
        WHERE c.embedding IS NOT NULL
        ORDER BY c.embedding <-> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)


                
    List<Chunk> findSimilarChunks(
    @Param("queryEmbedding") String queryEmbedding,
    @Param("limit") int limit
    );
  
}
