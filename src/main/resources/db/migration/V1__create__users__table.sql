CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (role IN ('USER', 'ADMIN'))
);


CREATE INDEX idx_users_email_ci ON users (LOWER(email));
