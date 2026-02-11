CREATE TABLE chat_sessions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_messages (
    id SERIAL PRIMARY KEY,
    session_id INTEGER NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    chunks_used JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    CHECK (chunks_used IS NULL OR jsonb_typeof(chunks_used) = 'array')
);

-- Indexes
CREATE INDEX idx_chat_sessions_user_id
ON chat_sessions(user_id);

CREATE INDEX idx_chat_messages_session_time
ON chat_messages(session_id, created_at);
