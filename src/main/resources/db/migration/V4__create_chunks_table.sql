CREATE TABLE chunks (
    if SERIAL PRIMARY KEY,
    document_id INTEGER NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(384),
    token_count INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, chunk_index)

);

-- document and ordering access
CREATE INDEX idx_chunks_document_order 
ON chunks(document_id, chunk_index);

-- vector similarity index (critical for RAG performance)
CREATE INDEX idx_chunks_embedding_ivfflat
ON chunks USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Note: For production with more data, consider HNSW index instead:
-- CREATE INDEX ON chunks USING hnsw (embedding vector_cosine_ops);